package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    
    private fun sanitizeId(id: String) = id.replace("/", "_").replace(".", "_")

    private fun getCommentsColl(eventId: String) = getExtensionsColl().document(sanitizeId(eventId)).collection("comments")

    suspend fun saveExtension(extension: CalendarEventExtension) {
        if (extension.googleEventId.isEmpty()) return
        getExtensionsColl().document(sanitizeId(extension.googleEventId)).set(extension, SetOptions.merge()).await()
    }

    suspend fun getExtension(googleEventId: String): CalendarEventExtension? {
        if (googleEventId.isEmpty()) return null
        val clean = googleEventId.removePrefix("ext_")
        return try {
            val direct = getExtensionsColl().document(sanitizeId(clean)).get().await().toObject(CalendarEventExtension::class.java)
            if (direct != null) return direct
            val byGoogle = getExtensionsColl().whereEqualTo("googleEventId", clean).limit(1).get().await()
                .documents.firstOrNull()?.toObject(CalendarEventExtension::class.java)
            if (byGoogle != null) return byGoogle
            getExtensionsColl().whereEqualTo("localEventId", clean).limit(1).get().await()
                .documents.firstOrNull()?.toObject(CalendarEventExtension::class.java)
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
        val clean = googleEventId.removePrefix("ext_")
        try {
            getExtensionsColl().document(sanitizeId(googleEventId)).delete().await()
            if (clean != googleEventId) {
                getExtensionsColl().document(sanitizeId(clean)).delete().await()
            }
            getExtensionsColl().whereEqualTo("googleEventId", clean).get().await().documents.forEach {
                it.reference.delete().await()
            }
            getExtensionsColl().whereEqualTo("localEventId", clean).get().await().documents.forEach {
                it.reference.delete().await()
            }
        } catch (_: Exception) {}
    }

    suspend fun addComment(googleEventId: String, comment: Comment) {
        if (googleEventId.isEmpty()) return
        getCommentsColl(googleEventId).document(comment.id).set(comment).await()
    }

    fun getCommentsCollection(googleEventId: String) = getCommentsColl(googleEventId)

    private fun indexExtension(
        map: MutableMap<String, CalendarEventExtension>,
        docId: String,
        ext: CalendarEventExtension
    ) {
        val primary = ext.googleEventId.ifEmpty { docId }
        val clean = primary.removePrefix("ext_")
        map[docId] = ext
        map[primary] = ext
        if (clean.isNotEmpty()) {
            map[clean] = ext
            map["ext_$clean"] = ext
        }
        if (ext.googleEventId.isNotEmpty()) {
            val cleanGoogle = ext.googleEventId.removePrefix("ext_")
            map[ext.googleEventId] = ext
            if (cleanGoogle.isNotEmpty()) {
                map[cleanGoogle] = ext
                map["ext_$cleanGoogle"] = ext
            }
        }
        if (ext.localEventId.isNotEmpty()) {
            val cleanLocal = ext.localEventId.removePrefix("ext_")
            map[ext.localEventId] = ext
            if (cleanLocal.isNotEmpty()) {
                map[cleanLocal] = ext
                map["ext_$cleanLocal"] = ext
            }
        }
    }

    suspend fun getExtensionsForUser(userId: String, groupIds: List<String>, startMs: Long? = null, endMs: Long? = null): Map<String, CalendarEventExtension> {
        val map = mutableMapOf<String, CalendarEventExtension>()
        if (userId.isNotEmpty()) {
            try {
                var ownerQuery: Query = getExtensionsColl().whereEqualTo("ownerUid", userId)
                if (startMs != null && endMs != null) {
                    ownerQuery = ownerQuery.whereGreaterThanOrEqualTo(CalendarEventExtension::dateTimestamp.name, startMs)
                        .whereLessThanOrEqualTo(CalendarEventExtension::dateTimestamp.name, endMs)
                }
                val ownerSnapshot = ownerQuery.get().await()
                ownerSnapshot.documents.forEach { doc ->
                    val ext = doc.toObject(CalendarEventExtension::class.java)
                    if (ext != null) indexExtension(map, doc.id, ext)
                }
            } catch (_: Exception) {}
        }

        if (groupIds.isNotEmpty()) {
            groupIds.chunked(10).forEach { chunk ->
                try {
                    var groupQuery: Query = getExtensionsColl().whereArrayContainsAny("sharedGroupIds", chunk)
                    if (startMs != null && endMs != null) {
                        groupQuery = groupQuery.whereGreaterThanOrEqualTo(CalendarEventExtension::dateTimestamp.name, startMs)
                            .whereLessThanOrEqualTo(CalendarEventExtension::dateTimestamp.name, endMs)
                    }
                    val groupSnapshot = groupQuery.get().await()
                    groupSnapshot.documents.forEach { doc ->
                        val ext = doc.toObject(CalendarEventExtension::class.java)
                        if (ext != null) indexExtension(map, doc.id, ext)
                    }
                } catch (_: Exception) {}
            }
        }
        return map
    }

    suspend fun getAllExtensions(userId: String = "", groupIds: List<String> = emptyList()): Map<String, CalendarEventExtension> {
        return getExtensionsForUser(userId, groupIds)
    }

    suspend fun getExtensionsForRange(userId: String = "", groupIds: List<String> = emptyList(), startMs: Long, endMs: Long): Map<String, CalendarEventExtension> {
        return getExtensionsForUser(userId, groupIds, startMs, endMs)
    }
}
