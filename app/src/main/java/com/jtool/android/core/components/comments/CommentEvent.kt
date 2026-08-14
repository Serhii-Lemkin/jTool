package com.jtool.android.core.components.comments

sealed interface CommentEvent {
    data class Add(val content: String, val parentId: String?) : CommentEvent
    data class Edit(val commentId: String, val newContent: String) : CommentEvent
    data class Delete(val commentId: String) : CommentEvent
}
