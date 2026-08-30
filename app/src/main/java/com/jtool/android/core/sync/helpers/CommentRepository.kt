package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.jtool.android.core.models.common.Comment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CommentRepository {
    fun getCommentsFlow(collection: CollectionReference): Flow<List<Comment>> = callbackFlow {
        val listener = collection.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)?.copy(id = doc.id)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(collection: CollectionReference, comment: Comment) {
        collection.document(comment.id).set(comment).await()
    }

    suspend fun updateComment(collection: CollectionReference, commentId: String, content: String) {
        collection.document(commentId).update(
            Comment::content.name, content,
            Comment::isEdited.name, true,
            Comment::timestamp.name, System.currentTimeMillis()
        ).await()
    }

    suspend fun deleteComment(collection: CollectionReference, commentId: String) {
        collection.document(commentId).delete().await()
    }
}
