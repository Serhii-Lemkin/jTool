package com.jtool.android.core.components.comments

import com.google.firebase.firestore.CollectionReference
import com.jtool.android.core.models.Comment
import com.jtool.android.core.sync.helpers.CommentRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class CommentController(
    private val repository: CommentRepository,
    private val scope: CoroutineScope,
    private val currentUserIdProvider: () -> String
) {
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var activeCollection: CollectionReference? = null
    private var listenerJob: Job? = null
    private val pendingComments = MutableStateFlow<List<Comment>>(emptyList())

    fun observe(collection: CollectionReference) {
        if (activeCollection?.path == collection.path) return

        activeCollection = collection
        listenerJob?.cancel()
        _isLoading.value = true

        listenerJob = repository.getCommentsFlow(collection)
            .onEach { serverComments ->
                // Filter out pending comments that have been confirmed by the server
                val serverIds = serverComments.map { it.id }.toSet()
                pendingComments.update { current ->
                    current.filter { it.id !in serverIds }
                }
                
                combineAndEmit(serverComments, pendingComments.value)
                _isLoading.value = false
            }
            .launchIn(scope)
    }

    private fun combineAndEmit(serverComments: List<Comment>, pending: List<Comment>) {
        val all = serverComments + pending
        _comments.value = buildCommentTree(all)
    }

    fun addComment(content: String, parentId: String?) {
        val collection = activeCollection ?: return
        val userId = currentUserIdProvider()
        val realId = collection.document().id
        
        val ghost = Comment(
            id = realId,
            content = content,
            userId = userId,
            timestamp = System.currentTimeMillis(),
            parentId = parentId,
            isPending = true
        )

        // Optimistic UI update
        pendingComments.update { it + ghost }
        combineAndEmit(_comments.value.flatten(), pendingComments.value)

        scope.launch {
            try {
                val confirmedComment = ghost.copy(isPending = false)
                repository.addComment(collection, confirmedComment)
                // The listener will handle removing from pending once it sees the server version
            } catch (e: Exception) {
                // Remove from pending on failure
                pendingComments.update { current -> current.filter { it.id != realId } }
                combineAndEmit(_comments.value.flatten(), pendingComments.value)
            }
        }
    }

    fun editComment(commentId: String, newContent: String) {
        val collection = activeCollection ?: return
        scope.launch {
            try {
                repository.updateComment(collection, commentId, newContent)
            } catch (e: Exception) { }
        }
    }

    fun deleteComment(commentId: String) {
        val collection = activeCollection ?: return
        scope.launch {
            try {
                repository.deleteComment(collection, commentId)
            } catch (e: Exception) { }
        }
    }

    private fun buildCommentTree(allComments: List<Comment>): List<Comment> {
        val roots = mutableListOf<Comment>()
        val children = mutableMapOf<String, MutableList<Comment>>()

        allComments.forEach { comment ->
            val pId = comment.parentId
            if (pId == null) {
                roots.add(comment)
            } else {
                children.getOrPut(pId) { mutableListOf() }.add(comment)
            }
        }

        fun assemble(comment: Comment): Comment {
            val replies = children[comment.id]
                ?.map { assemble(it) }
                ?.sortedBy { it.timestamp } ?: emptyList()
            return comment.copy(replies = replies)
        }

        return roots.map { assemble(it) }.sortedByDescending { it.timestamp }
    }

    // Helper to flatten the tree for merging logic
    private fun List<Comment>.flatten(): List<Comment> {
        val result = mutableListOf<Comment>()
        fun walk(list: List<Comment>) {
            list.forEach {
                result.add(it)
                walk(it.replies)
            }
        }
        walk(this)
        return result
    }

    fun stop() {
        listenerJob?.cancel()
        activeCollection = null
        _comments.value = emptyList()
        pendingComments.value = emptyList()
    }
}
