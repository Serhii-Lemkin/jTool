package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.core.models.tracker.TrackerRecord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TrackerDatabaseHelper(private val firestore: FirebaseFirestore) {

    private fun getGroupsColl() = firestore.collection("groups")
    private fun getTrackersColl(groupId: String) = getGroupsColl().document(groupId).collection("trackers")
    private fun getRecordsColl(groupId: String) = getGroupsColl().document(groupId).collection("tracker_records")

    fun getTrackersFlow(groupId: String): Flow<List<Tracker>> = callbackFlow {
        val listener = getTrackersColl(groupId)
            .orderBy(Tracker::createdAt.name, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Tracker::class.java)?.copy(id = it.id) }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getRecordsFlow(groupId: String, trackerId: String): Flow<List<TrackerRecord>> = callbackFlow {
        val listener = getRecordsColl(groupId)
            .whereEqualTo(TrackerRecord::trackerId.name, trackerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(TrackerRecord::class.java)?.copy(id = it.id) }
                    trySend(list.sortedBy { it.timestamp })
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTracker(groupId: String, tracker: Tracker): String {
        val docRef = getTrackersColl(groupId).document()
        val finalTracker = tracker.copy(id = docRef.id, createdAt = System.currentTimeMillis())
        docRef.set(finalTracker).await()
        return docRef.id
    }

    suspend fun updateTracker(groupId: String, tracker: Tracker) {
        getTrackersColl(groupId).document(tracker.id).set(tracker).await()
    }

    suspend fun deleteTracker(groupId: String, trackerId: String) {
        getTrackersColl(groupId).document(trackerId).delete().await()
        // Batch delete records
        val records = getRecordsColl(groupId).whereEqualTo(TrackerRecord::trackerId.name, trackerId).get().await()
        firestore.runBatch { batch ->
            records.documents.forEach { batch.delete(it.reference) }
        }.await()
    }

    suspend fun addRecord(groupId: String, record: TrackerRecord): String {
        val docRef = getRecordsColl(groupId).document()
        val finalRecord = record.copy(id = docRef.id)
        docRef.set(finalRecord).await()
        return docRef.id
    }

    suspend fun updateRecord(groupId: String, record: TrackerRecord) {
        getRecordsColl(groupId).document(record.id).set(record).await()
    }

    suspend fun deleteRecord(groupId: String, recordId: String) {
        getRecordsColl(groupId).document(recordId).delete().await()
    }
}
