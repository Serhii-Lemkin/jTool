package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.UserData
import com.jtool.android.features.support.logic.SupportRequest
import com.jtool.android.features.support.logic.SupportStatus
import com.jtool.android.features.support.logic.SupportType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SupportDatabaseHelper(private val firestore: FirebaseFirestore) {

    private fun getSupportColl() = firestore.collection("support_requests")
    private fun getOptionsDoc() = firestore.collection("app_options").document("config")
    private fun getSupportDataDoc() = firestore.collection("app_options").document("support_data")
    private fun getUserDataColl() = firestore.collection("user_data")

    suspend fun getAppOptions(): Map<String, Any>? {
        return try {
            val snapshot = getOptionsDoc().get().await()
            if (snapshot.exists()) snapshot.data else {
                // Initialize if missing
                val default = mapOf("max_requests" to 2, "privacy" to "https://docs.google.com/document/d/e/2PACX-1vSdi0XIvbx21ot5QK0o8gEbrj-IPcwsclGR_2AuraXHcKiKZFFn4_vdQdJlgkrs0dvEw3bPWqRkdDir/pub")
                getOptionsDoc().set(default).await()
                default
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserData(uid: String): UserData? {
        return try {
            getUserDataColl().document(uid).get().await().toObject(UserData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitRequest(request: SupportRequest, uid: String, bypassLimit: Boolean = false): Boolean {
        return try {
            firestore.runTransaction { transaction ->
                // Check if User is Admin
                val isAdmin = if (bypassLimit) true else {
                    val userRef = firestore.collection("users").document(uid)
                    val userSnapshot = transaction.get(userRef)
                    userSnapshot.getBoolean("isAdmin") ?: false
                }

                // Check Rate Limit (Skip for Admin or Bypass)
                val userDataRef = getUserDataColl().document(uid)
                val userDataSnapshot = transaction.get(userDataRef)
                val userData = userDataSnapshot.toObject(UserData::class.java) ?: UserData()
                
                val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
                val currentCount = if (userData.lastSupportDate == today) userData.dailySupportCount else 0
                
                // Get Max Requests from config
                val optionsRef = getOptionsDoc()
                val optionsSnapshot = transaction.get(optionsRef)
                val maxRequests = optionsSnapshot.getLong("max_requests") ?: 2L
                
                if (!isAdmin && currentCount >= maxRequests) {
                    throw Exception("LIMIT_REACHED")
                }

                // Increment Counter
                val supportDataRef = getSupportDataDoc()
                val supportDataSnapshot = transaction.get(supportDataRef)
                val lastNumber = supportDataSnapshot.getLong("last_support_ticket_number") ?: 0L
                val newNumber = lastNumber + 1
                
                transaction.set(supportDataRef, mapOf("last_support_ticket_number" to newNumber), SetOptions.merge())
                
                // Save Request
                val finalRequest = request.copy(number = newNumber.toInt())
                val newDocRef = getSupportColl().document()
                transaction.set(newDocRef, finalRequest.copy(id = newDocRef.id))
                
                // Update User Meta
                transaction.set(userDataRef, UserData(lastSupportDate = today, dailySupportCount = currentCount + 1), SetOptions.merge())
            }.await()
            true
        } catch (e: Exception) {
            if (e.message == "LIMIT_REACHED") {
                android.util.Log.w("SupportDB", "Rate limit reached for user $uid")
            } else {
                android.util.Log.e("SupportDB", "Transaction failed", e)
            }
            throw e // Rethrow to let ViewModel handle specific error
        }
    }

    fun getRequestsFlow(isAdmin: Boolean, userId: String): Flow<List<SupportRequest>> = callbackFlow {
        val query = if (isAdmin) {
            getSupportColl().orderBy("timestamp", Query.Direction.DESCENDING)
        } else {
            getSupportColl().whereEqualTo("userId", userId).orderBy("timestamp", Query.Direction.DESCENDING)
        }

        val listener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                // To fetch comments in a flow of a list, we need a nested approach or just fetch them here.
                // Since Firestore listeners can't be easily nested in a single callbackFlow, we do a flat map in ViewModel
                val list = snapshot.documents.mapNotNull { it.toObject(SupportRequest::class.java)?.copy(id = it.id) }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateStatus(requestId: String, status: SupportStatus) {
        getSupportColl().document(requestId).update("status", status.name).await()
    }

    suspend fun updateType(requestId: String, type: SupportType) {
        getSupportColl().document(requestId).update("type", type.name).await()
    }

    fun getCommentsFlow(requestId: String): Flow<List<com.jtool.android.core.models.Comment>> = callbackFlow {
        val listener = getSupportColl().document(requestId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val allComments = snapshot.documents.mapNotNull { it.toObject(com.jtool.android.core.models.Comment::class.java)?.copy(id = it.id) }
                    trySend(buildCommentTree(allComments))
                }
            }
        awaitClose { listener.remove() }
    }

    private fun buildCommentTree(allComments: List<com.jtool.android.core.models.Comment>): List<com.jtool.android.core.models.Comment> {
        val roots = mutableListOf<com.jtool.android.core.models.Comment>()
        val children = mutableMapOf<String, MutableList<com.jtool.android.core.models.Comment>>()

        allComments.forEach { comment ->
            if (comment.parentId == null) {
                roots.add(comment)
            } else {
                children.getOrPut(comment.parentId) { mutableListOf() }.add(comment)
            }
        }

        fun assemble(comment: com.jtool.android.core.models.Comment): com.jtool.android.core.models.Comment {
            val replies = children[comment.id]
                ?.map { assemble(it) }
                ?.sortedBy { it.timestamp } ?: emptyList()
            return comment.copy(replies = replies)
        }

        return roots.map { assemble(it) }.sortedByDescending { it.timestamp }
    }

    suspend fun addComment(requestId: String, comment: com.jtool.android.core.models.Comment) {
        val docRef = getSupportColl().document(requestId)
        docRef.collection("comments").document(comment.id).set(comment).await()
        docRef.update("timestamp", System.currentTimeMillis()).await()
    }

    suspend fun editComment(requestId: String, commentId: String, newContent: String) {
        val docRef = getSupportColl().document(requestId)
        docRef.collection("comments").document(commentId)
            .update("content", newContent, "isEdited", true, "timestamp", System.currentTimeMillis()).await()
        docRef.update("timestamp", System.currentTimeMillis()).await()
    }

    suspend fun deleteComment(requestId: String, commentId: String) {
        val docRef = getSupportColl().document(requestId)
        docRef.collection("comments").document(commentId).delete().await()
        docRef.update("timestamp", System.currentTimeMillis()).await()
    }

    suspend fun deleteRequest(requestId: String) {
        getSupportColl().document(requestId).delete().await()
    }

    suspend fun migrateLegacyRequests(onProgress: (String) -> Unit): Boolean {
        return try {
            val suggestionsColl = firestore.collection("suggestions")
            val snapshot = suggestionsColl.get().await()
            val total = snapshot.size()
            var count = 0
            
            snapshot.documents.forEach { doc ->
                val text = doc.getString("text") ?: ""
                val userId = doc.getString("userId") ?: "unknown"
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                
                val request = SupportRequest(
                    type = SupportType.BUG, // Fallback
                    status = SupportStatus.CLOSED, // Old ones probably done
                    text = text,
                    userId = userId,
                    userName = "Migrated Legacy",
                    timestamp = timestamp,
                    deviceInfo = mapOf("migrated" to "true")
                )
                
                submitRequest(request, userId, bypassLimit = true)
                suggestionsColl.document(doc.id).delete().await()
                count++
                onProgress("Migrated $count/$total")
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("SupportDB", "Migration failed", e)
            false
        }
    }
}
