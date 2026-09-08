package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.user.UserData
import com.jtool.android.core.models.support.SupportConfig
import com.jtool.android.core.models.support.SupportTicketMeta
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SupportDatabaseHelper(private val firestore: FirebaseFirestore) {

    private fun getSupportColl() = firestore.collection(SupportConfig.COLLECTION_NAME)
    private fun getOptionsDoc() = firestore.collection(SupportConfig.APP_OPTIONS_COLLECTION).document(SupportConfig.CONFIG_DOCUMENT)
    private fun getSupportDataDoc() = firestore.collection(SupportConfig.APP_OPTIONS_COLLECTION).document(SupportConfig.SUPPORT_DATA_DOCUMENT)
    private fun getUserDataColl() = firestore.collection(SupportConfig.USER_DATA_COLLECTION)

    suspend fun getAppOptions(): SupportConfig? {
        return try {
            val snapshot = getOptionsDoc().get().await()
            snapshot.toObject(SupportConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitRequest(request: SupportRequest, uid: String, bypassLimit: Boolean = false): Boolean {
        return try {
            // De-duplication check
            val existing = getSupportColl()
                .whereEqualTo(SupportRequest::userId.name, uid)
                .whereEqualTo(SupportRequest::type.name, request.type.name)
                .whereEqualTo(SupportRequest::text.name, request.text)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                val existingRequest = existing.documents.first().toObject(SupportRequest::class.java)
                if (existingRequest?.status == SupportStatusEnum.OPENED || existingRequest?.status == SupportStatusEnum.IN_PROGRESS) {
                    return true 
                }
            }

            firestore.runTransaction { transaction ->
                val isAdmin = if (bypassLimit) true else {
                    val userRef = firestore.collection(SupportConfig.USERS_COLLECTION).document(uid)
                    val userSnapshot = transaction.get(userRef)
                    userSnapshot.toObject(com.jtool.android.core.models.user.User::class.java)?.isAdmin ?: false
                }

                val userDataRef = getUserDataColl().document(uid)
                val userDataSnapshot = transaction.get(userDataRef)
                val userData = userDataSnapshot.toObject(UserData::class.java) ?: UserData()
                
                val today = java.text.SimpleDateFormat(SupportConfig.DATE_FORMAT, java.util.Locale.US).format(java.util.Date())
                val currentCount = if (userData.lastSupportDate == today) userData.dailySupportCount else 0
                
                val optionsRef = getOptionsDoc()
                val optionsSnapshot = transaction.get(optionsRef)
                val config = optionsSnapshot.toObject(SupportConfig::class.java)
                val maxRequests = config?.max_requests ?: 2L
                
                if (!isAdmin && currentCount >= maxRequests) {
                    throw Exception(SupportConfig.ERROR_LIMIT_REACHED)
                }

                val supportDataRef = getSupportDataDoc()
                val supportDataSnapshot = transaction.get(supportDataRef)
                val meta = supportDataSnapshot.toObject(SupportTicketMeta::class.java)
                val newNumber = (meta?.last_support_ticket_number ?: 0L) + 1
                
                transaction.set(supportDataRef, SupportTicketMeta(last_support_ticket_number = newNumber), SetOptions.merge())
                
                val finalRequest = request.copy(number = newNumber.toInt())
                val newDocRef = getSupportColl().document()
                transaction.set(newDocRef, finalRequest.copy(id = newDocRef.id))
                
                val isCrash = request.type == SupportTypeEnum.CRASH
                val increment = if (isCrash) 0 else 1
                
                transaction.set(userDataRef, userData.copy(lastSupportDate = today, dailySupportCount = currentCount + increment), SetOptions.merge())
            }.await()
            true
        } catch (e: Exception) {
            throw e
        }
    }

    fun getRequestsFlow(isAdmin: Boolean, userId: String): Flow<List<SupportRequest>> = callbackFlow {
        val query = if (isAdmin) {
            getSupportColl().orderBy(SupportRequest::timestamp.name, Query.Direction.DESCENDING)
        } else {
            getSupportColl().whereEqualTo(SupportRequest::userId.name, userId).orderBy(SupportRequest::timestamp.name, Query.Direction.DESCENDING)
        }

        val listener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(SupportRequest::class.java)?.copy(id = it.id) }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateStatus(requestId: String, status: SupportStatusEnum) {
        getSupportColl().document(requestId).update(
            SupportRequest::status.name, status.name,
            SupportRequest::timestamp.name, System.currentTimeMillis()
        ).await()
    }

    suspend fun updateType(requestId: String, type: SupportTypeEnum) {
        getSupportColl().document(requestId).update(
            SupportRequest::type.name, type.name,
            SupportRequest::timestamp.name, System.currentTimeMillis()
        ).await()
    }

    fun getCommentsFlow(requestId: String): Flow<List<com.jtool.android.core.models.common.Comment>> = callbackFlow {
        val listener = getSupportColl().document(requestId).collection(SupportConfig.COMMENTS_COLLECTION)
            .orderBy(com.jtool.android.core.models.common.Comment::timestamp.name, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val allComments = snapshot.documents.mapNotNull { it.toObject(com.jtool.android.core.models.common.Comment::class.java)?.copy(id = it.id) }
                    trySend(buildCommentTree(allComments))
                }
            }
        awaitClose { listener.remove() }
    }

    private fun buildCommentTree(allComments: List<com.jtool.android.core.models.common.Comment>): List<com.jtool.android.core.models.common.Comment> {
        val roots = mutableListOf<com.jtool.android.core.models.common.Comment>()
        val children = mutableMapOf<String, MutableList<com.jtool.android.core.models.common.Comment>>()

        allComments.forEach { comment ->
            if (comment.parentId == null) {
                roots.add(comment)
            } else {
                children.getOrPut(comment.parentId) { mutableListOf() }.add(comment)
            }
        }

        fun assemble(comment: com.jtool.android.core.models.common.Comment): com.jtool.android.core.models.common.Comment {
            val replies = children[comment.id]
                ?.map { assemble(it) }
                ?.sortedBy { it.timestamp } ?: emptyList()
            return comment.copy(replies = replies)
        }

        return roots.map { assemble(it) }.sortedByDescending { it.timestamp }
    }

    suspend fun addComment(requestId: String, comment: com.jtool.android.core.models.common.Comment) {
        val docRef = getSupportColl().document(requestId)
        docRef.collection(SupportConfig.COMMENTS_COLLECTION).document(comment.id).set(comment).await()
        docRef.update(SupportRequest::timestamp.name, System.currentTimeMillis()).await()
    }

    suspend fun editComment(requestId: String, commentId: String, newContent: String) {
        val docRef = getSupportColl().document(requestId)
        docRef.collection(SupportConfig.COMMENTS_COLLECTION).document(commentId)
            .update(
                com.jtool.android.core.models.common.Comment::content.name, newContent,
                com.jtool.android.core.models.common.Comment::isEdited.name, true,
                com.jtool.android.core.models.common.Comment::timestamp.name, System.currentTimeMillis()
            ).await()
        docRef.update(SupportRequest::timestamp.name, System.currentTimeMillis()).await()
    }

    suspend fun deleteComment(requestId: String, commentId: String) {
        val docRef = getSupportColl().document(requestId)
        docRef.collection(SupportConfig.COMMENTS_COLLECTION).document(commentId).delete().await()
        docRef.update(SupportRequest::timestamp.name, System.currentTimeMillis()).await()
    }

    suspend fun deleteRequest(requestId: String) {
        getSupportColl().document(requestId).delete().await()
    }
}
