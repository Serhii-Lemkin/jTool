package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jtool.android.core.models.workday.WorkHistoryItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WorkdayDatabaseHelper(private val firestore: FirebaseFirestore) {

    private fun getHistoryColl(userId: String) = 
        firestore.collection("users").document(userId).collection("work_history")

    suspend fun addHistoryItem(userId: String, item: WorkHistoryItem) {
        val coll = getHistoryColl(userId)
        val docRef = if (item.id.isEmpty()) coll.document() else coll.document(item.id)
        docRef.set(item.copy(id = docRef.id)).await()
    }

    fun getHistoryFlow(userId: String, fromDate: String, toDate: String): Flow<List<WorkHistoryItem>> = callbackFlow {
        val query = getHistoryColl(userId)
            .whereGreaterThanOrEqualTo(WorkHistoryItem::date.name, fromDate)
            .whereLessThanOrEqualTo(WorkHistoryItem::date.name, toDate)
            .orderBy(WorkHistoryItem::date.name, Query.Direction.DESCENDING)
            // Removed secondary timestamp orderBy to avoid index requirement for simple range

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("WorkdayDB", "History fetch failed: ${error.message}", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(WorkHistoryItem::class.java)?.copy(id = it.id) }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun deleteHistoryItem(userId: String, itemId: String) {
        getHistoryColl(userId).document(itemId).delete().await()
    }

    suspend fun clearHistory(userId: String) {
        val coll = getHistoryColl(userId)
        val snapshot = coll.get().await()
        firestore.runBatch { batch ->
            snapshot.documents.forEach { batch.delete(it.reference) }
        }.await()
    }
}
