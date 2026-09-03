package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CalendarDatabaseHelper(private val firestore: FirebaseFirestore) {

    private fun getPrivateEventsColl(userId: String) = 
        firestore.collection("user_data").document(userId).collection("private_calendar_events")

    private fun getSharedEventsColl(groupId: String) = 
        firestore.collection("groups").document(groupId).collection("shared_calendar_events")

    suspend fun addPrivateEvent(userId: String, event: CalendarEvent) {
        val coll = getPrivateEventsColl(userId)
        val docRef = if (event.id.isEmpty()) coll.document() else coll.document(event.id)
        docRef.set(event.copy(id = docRef.id)).await()
    }

    suspend fun addSharedEvent(groupId: String, event: CalendarEvent) {
        val coll = getSharedEventsColl(groupId)
        val docRef = if (event.id.isEmpty()) coll.document() else coll.document(event.id)
        docRef.set(event.copy(id = docRef.id)).await()
    }

    fun getPrivateEventsFlow(userId: String, fromDate: String, toDate: String): Flow<List<CalendarEvent>> = callbackFlow {
        val query = getPrivateEventsColl(userId)
            .whereGreaterThanOrEqualTo(CalendarEvent::date.name, fromDate)
            .whereLessThanOrEqualTo(CalendarEvent::date.name, toDate)
            .orderBy(CalendarEvent::date.name, Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(CalendarEvent::class.java)?.copy(id = it.id) }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    fun getSharedEventsFlow(groupId: String, fromDate: String, toDate: String): Flow<List<CalendarEvent>> = callbackFlow {
        val query = getSharedEventsColl(groupId)
            .whereGreaterThanOrEqualTo(CalendarEvent::date.name, fromDate)
            .whereLessThanOrEqualTo(CalendarEvent::date.name, toDate)
            .orderBy(CalendarEvent::date.name, Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(CalendarEvent::class.java)?.copy(id = it.id) }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun deletePrivateEvent(userId: String, eventId: String) {
        getPrivateEventsColl(userId).document(eventId).delete().await()
    }

    suspend fun deleteSharedEvent(groupId: String, eventId: String) {
        getSharedEventsColl(groupId).document(eventId).delete().await()
    }

    suspend fun clearPrivateShiftsInRange(userId: String, fromDate: String, toDate: String) {
        val coll = getPrivateEventsColl(userId)
        val snapshot = coll
            .whereGreaterThanOrEqualTo(CalendarEvent::date.name, fromDate)
            .whereLessThanOrEqualTo(CalendarEvent::date.name, toDate)
            .get().await()
            
        if (snapshot.isEmpty) return

        firestore.runBatch { batch ->
            snapshot.documents.forEach { doc ->
                val type = doc.getString(CalendarEvent::type.name)
                if (type == CalendarEventTypeEnum.SHIFT.name) {
                    batch.delete(doc.reference)
                }
            }
        }.await()
    }

    suspend fun replacePrivateShiftsInRange(userId: String, fromDate: String, toDate: String, newShifts: List<CalendarEvent>) {
        val coll = getPrivateEventsColl(userId)
        val snapshot = coll
            .whereGreaterThanOrEqualTo(CalendarEvent::date.name, fromDate)
            .whereLessThanOrEqualTo(CalendarEvent::date.name, toDate)
            .get().await()

        firestore.runBatch { batch ->
            // Delete old shifts
            snapshot.documents.forEach { doc ->
                val type = doc.getString(CalendarEvent::type.name)
                if (type == CalendarEventTypeEnum.SHIFT.name) {
                    batch.delete(doc.reference)
                }
            }
            // Add new shifts
            newShifts.forEach { event ->
                val docRef = if (event.id.isEmpty()) coll.document() else coll.document(event.id)
                batch.set(docRef, event.copy(id = docRef.id))
            }
        }.await()
    }
}
