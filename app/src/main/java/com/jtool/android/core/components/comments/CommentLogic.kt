package com.jtool.android.core.components.comments

import androidx.compose.runtime.*
import com.jtool.android.core.models.Comment

class CommentSectionState(
    initialExpanded: Boolean = false
) {
    var isExpanded by mutableStateOf(initialExpanded)
    var commentInput by mutableStateOf("")
    var replyingToId by mutableStateOf<String?>(null)

    fun onAddComment(onEvent: (CommentEvent) -> Unit) {
        if (commentInput.isNotBlank()) {
            onEvent(CommentEvent.Add(commentInput.trim(), replyingToId))
            commentInput = ""
            replyingToId = null
        }
    }
    
    fun setReplyingTo(id: String?) {
        replyingToId = id
    }
}

@Composable
fun rememberCommentSectionState(initialExpanded: Boolean = false) = remember {
    CommentSectionState(initialExpanded)
}

fun getTotalCommentsCount(comments: List<Comment>): Int {
    var count = comments.size
    comments.forEach { count += getTotalCommentsCount(it.replies) }
    return count
}
