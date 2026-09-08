package com.jtool.android.core.sync.helpers

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.common.Comment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CalendarDatabaseHelper(private val firestore: FirebaseFirestore) {

    private fun getEventsColl() = firestore.collection("calendar_events")

    // --- Unified Centralized Storage ---

    suspend fun saveEvent(event: CalendarEvent): String {
        val coll = getEventsColl()
        val docRef = if (event.id.isEmpty()) coll.document() else coll.document(event.id)
        val finalEvent = event.copy(id = docRef.id)
        docRef.set(finalEvent).await()
        return docRef.id
    }

    suspend fun deleteEvent(eventId: String) {
        if (eventId.isNotBlank()) {
            getEventsColl().document(eventId).delete().await()
        }
    }

    suspend fun deleteEventLocalOnly(eventId: String) {
        if (eventId.isNotBlank()) {
            getEventsColl().document(eventId).delete().await()
        }
    }

    fun getAllEventsFlow(userId: String, groupIds: List<String>, fromMs: Long, toMs: Long): Flow<List<CalendarEvent>> = callbackFlow {
        // Query events owned by user
        val ownerQuery = getEventsColl()
            .whereEqualTo(CalendarEvent::ownerUid.name, userId)
            .whereGreaterThanOrEqualTo(CalendarEvent::dtStart.name, fromMs)
            .whereLessThanOrEqualTo(CalendarEvent::dtStart.name, toMs)

        // Query events shared with user's groups
        // Firestore 'array-contains-any' is perfect here
        val sharedQuery = if (groupIds.isNotEmpty()) {
            getEventsColl()
                .whereArrayContainsAny(CalendarEvent::sharedGroupIds.name, groupIds)
                .whereGreaterThanOrEqualTo(CalendarEvent::dtStart.name, fromMs)
                .whereLessThanOrEqualTo(CalendarEvent::dtStart.name, toMs)
        } else null

        val listeners = mutableListOf<ListenerRegistration>()
        val resultsMap = mutableMapOf<String, List<CalendarEvent>>()

        fun emitCombined() {
            val all = resultsMap.values.flatten().distinctBy { it.id }
            trySend(all)
        }

        listeners.add(ownerQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("CalDBHelper", "Owner Query Error", error)
                resultsMap["owner"] = emptyList()
                emitCombined()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                resultsMap["owner"] = snapshot.documents.mapNotNull { it.toObject(CalendarEvent::class.java) }
                emitCombined()
            }
        })

        sharedQuery?.let {
            listeners.add(it.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CalDBHelper", "Shared Query Error", error)
                    resultsMap["shared"] = emptyList()
                    emitCombined()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    resultsMap["shared"] = snapshot.documents.mapNotNull { it.toObject(CalendarEvent::class.java) }
                    emitCombined()
                }
            })
        }

        awaitClose { listeners.forEach { it.remove() } }
    }

    suspend fun getAllEventsOneTime(userId: String, groupIds: List<String>, fromMs: Long, toMs: Long): List<CalendarEvent> {
        return try {
            val ownerSnap = getEventsColl()
                .whereEqualTo(CalendarEvent::ownerUid.name, userId)
                .whereGreaterThanOrEqualTo(CalendarEvent::dtStart.name, fromMs)
                .whereLessThanOrEqualTo(CalendarEvent::dtStart.name, toMs)
                .get().await()

            val ownerEvents = ownerSnap.documents.mapNotNull { it.toObject(CalendarEvent::class.java) }

            val sharedEvents = if (groupIds.isNotEmpty()) {
                val chunks = groupIds.chunked(10)
                val allShared = mutableListOf<CalendarEvent>()
                for (chunk in chunks) {
                    val sharedSnap = getEventsColl()
                        .whereArrayContainsAny(CalendarEvent::sharedGroupIds.name, chunk)
                        .whereGreaterThanOrEqualTo(CalendarEvent::dtStart.name, fromMs)
                        .whereLessThanOrEqualTo(CalendarEvent::dtStart.name, toMs)
                        .get().await()
                    allShared.addAll(sharedSnap.documents.mapNotNull { it.toObject(CalendarEvent::class.java) })
                }
                allShared
            } else emptyList()

            (ownerEvents + sharedEvents).distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("CalDBHelper", "One-time fetch failed", e)
            emptyList()
        }
    }

    // --- Comments (Centralized) ---

    fun getEventCommentsFlow(eventId: String): Flow<List<Comment>> = callbackFlow {
        val listener = getEventsColl().document(eventId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Comment::class.java)?.copy(id = it.id) }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(eventId: String, comment: Comment) {
        getEventsColl().document(eventId).collection("comments").document(comment.id).set(comment).await()
    }

    suspend fun updateComment(eventId: String, commentId: String, newContent: String) {
        getEventsColl().document(eventId).collection("comments").document(commentId)
            .update(
                Comment::content.name, newContent,
                Comment::isEdited.name, true,
                Comment::timestamp.name, System.currentTimeMillis()
            ).await()
    }

    suspend fun deleteComment(eventId: String, commentId: String) {
        getEventsColl().document(eventId).collection("comments").document(commentId).delete().await()
    }

    // --- Legacy Support & Bridge ---
    // We keep these for now to support the transition if needed, but they should be phased out.

    suspend fun clearShiftsInRange(userId: String, groupIds: List<String>, fromMs: Long, toMs: Long) {
        // Find and delete shifts in range
        val coll = getEventsColl()
        val snapshot = coll
            .whereEqualTo(CalendarEvent::ownerUid.name, userId)
            .whereEqualTo(CalendarEvent::type.name, CalendarEventTypeEnum.SHIFT.name)
            .whereGreaterThanOrEqualTo(CalendarEvent::dtStart.name, fromMs)
            .whereLessThanOrEqualTo(CalendarEvent::dtStart.name, toMs)
            .get().await()

        if (snapshot.isEmpty) return

        firestore.runBatch { batch ->
            snapshot.documents.forEach { batch.delete(it.reference) }
        }.await()
    }
    
    // Bridge methods to satisfy DataManager while refactoring
    fun getPrivateEventsFlow(userId: String, fromDate: String, toDate: String): Flow<List<CalendarEvent>> = callbackFlow {
        val listener = firestore.collection("user_data").document(userId).collection("private_calendar_events")
            .whereGreaterThanOrEqualTo(CalendarEvent::date.name, fromDate)
            .whereLessThanOrEqualTo(CalendarEvent::date.name, toDate)
            .addSnapshotListener { s, _ -> if(s != null) trySend(s.documents.mapNotNull { it.toObject(CalendarEvent::class.java) }) }
        awaitClose { listener.remove() }
    }

    fun getSharedEventsFlow(groupId: String, fromDate: String, toDate: String): Flow<List<CalendarEvent>> = callbackFlow {
        val listener = firestore.collection("groups").document(groupId).collection("shared_calendar_events")
            .whereGreaterThanOrEqualTo(CalendarEvent::date.name, fromDate)
            .whereLessThanOrEqualTo(CalendarEvent::date.name, toDate)
            .addSnapshotListener { s, _ -> if(s != null) trySend(s.documents.mapNotNull { it.toObject(CalendarEvent::class.java) }) }
        awaitClose { listener.remove() }
    }

    suspend fun replacePrivateShiftsInRange(userId: String, fromDate: String, toDate: String, newShifts: List<CalendarEvent>) {
        val coll = firestore.collection("user_data").document(userId).collection("private_calendar_events")
        val snapshot = coll
            .whereGreaterThanOrEqualTo(CalendarEvent::date.name, fromDate)
            .whereLessThanOrEqualTo(CalendarEvent::date.name, toDate)
            .get().await()

        firestore.runBatch { batch ->
            snapshot.documents.forEach { doc ->
                if (doc.getString(CalendarEvent::type.name) == CalendarEventTypeEnum.SHIFT.name) {
                    batch.delete(doc.reference)
                }
            }
            newShifts.forEach { event ->
                val docRef = if (event.id.isEmpty()) coll.document() else coll.document(event.id)
                batch.set(docRef, event.copy(id = docRef.id))
            }
        }.await()
    }
}
