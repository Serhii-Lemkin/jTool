package com.jtool.android.core.components.comments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.Comment

@Composable
fun CommentSection(
    comments: List<Comment>,
    currentUserId: String,
    userNames: Map<String, String>,
    onEvent: (CommentEvent) -> Unit,
    state: CommentSectionState = rememberCommentSectionState(),
    initiallyExpanded: Boolean = false,
    isOwner: (Comment) -> Boolean = { it.userId == currentUserId }
) {
    LaunchedEffect(Unit) {
        if (initiallyExpanded) state.isExpanded = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { state.isExpanded = !state.isExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Icon(
                imageVector = if (state.isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.scale(if (!state.isExpanded && isRtl) -1f else 1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${Loc.comments_comments} (${getTotalCommentsCount(comments)})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(visible = state.isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    if (state.replyingToId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = Loc.comments_replying,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { state.setReplyingTo(null) }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                        
                        val sendButton = @Composable {
                            IconButton(
                                onClick = { state.onAddComment(onEvent) },
                                enabled = state.commentInput.isNotBlank()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (isRtl) {
                            sendButton()
                            Spacer(Modifier.width(8.dp))
                        }

                        OutlinedTextField(
                            value = state.commentInput,
                            onValueChange = { state.commentInput = it },
                            placeholder = { Text(Loc.comments_addComment) },
                            modifier = Modifier.weight(1f),
                            maxLines = 3
                        )
                        
                        if (!isRtl) {
                            Spacer(Modifier.width(8.dp))
                            sendButton()
                        }
                    }
                }

                comments.forEach { comment ->
                    CommentItem(
                        comment = comment,
                        depth = 0,
                        currentUserId = currentUserId,
                        userNames = userNames,
                        onEvent = onEvent,
                        onReplyRequest = { state.setReplyingTo(it) },
                        isOwner = isOwner
                    )
                }
            }
        }
    }
}
