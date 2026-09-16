package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.common.Comment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CalendarExtensionDatabaseHelper(
    private val firestore: FirebaseFirestore
) {
    private fun getExtensionsColl() = firestore.collection("calendar_event_extensions")
    
    // Sanitize document ID to prevent slashes from breaking Firestore collection structure
    private fun sanitizeId(id: String) = id.replace("/", "_").replace(".", "_")
    
    private fun getCommentsColl(eventId: String) = getExtensionsColl().document(sanitizeId(eventId)).collection("comments")

    suspend fun saveExtension(extension: CalendarEventExtension) {
        if (extension.googleEventId.isEmpty()) return
        getExtensionsColl().document(sanitizeId(extension.googleEventId)).set(extension, SetOptions.merge()).await()
    }

    suspend fun getExtension(googleEventId: String): CalendarEventExtension? {
        if (googleEventId.isEmpty()) return null
        return try {
            getExtensionsColl().document(sanitizeId(googleEventId)).get().await().toObject(CalendarEventExtension::class.java)
        } catch (e: Exception) { null }
    }

    fun getExtensionFlow(googleEventId: String): Flow<CalendarEventExtension?> = callbackFlow {
        if (googleEventId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = getExtensionsColl().document(sanitizeId(googleEventId))
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            val ext = snapshot?.toObject(CalendarEventExtension::class.java)
            trySend(ext)
        }
        awaitClose { listener.remove() }
    }

    suspend fun deleteExtension(googleEventId: String) {
        if (googleEventId.isEmpty()) return
        getExtensionsColl().document(sanitizeId(googleEventId)).delete().await()
    }

    suspend fun addComment(googleEventId: String, comment: Comment) {
        if (googleEventId.isEmpty()) return
        getCommentsColl(googleEventId).document(comment.id).set(comment).await()
    }

    fun getCommentsCollection(googleEventId: String) = getCommentsColl(googleEventId)

    suspend fun getAllExtensions(): Map<String, CalendarEventExtension> {
        return try {
            val snapshot = getExtensionsColl().get().await()
            val map = mutableMapOf<String, CalendarEventExtension>()
            snapshot.documents.forEach { doc ->
                val ext = doc.toObject(CalendarEventExtension::class.java)
                if (ext != null) {
                    val id = ext.googleEventId.ifEmpty { doc.id }
                    map[id] = ext
                    if (ext.googleEventId.isNotEmpty()) map[ext.googleEventId] = ext
                }
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getExtensionsForRange(startMs: Long, endMs: Long): Map<String, CalendarEventExtension> {
        return try {
            val snapshot = getExtensionsColl()
                .whereGreaterThanOrEqualTo(CalendarEventExtension::dateTimestamp.name, startMs)
                .whereLessThanOrEqualTo(CalendarEventExtension::dateTimestamp.name, endMs)
                .get()
                .await()
            
            snapshot.documents.associate { doc ->
                val ext = doc.toObject(CalendarEventExtension::class.java)!!
                ext.googleEventId to ext
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
