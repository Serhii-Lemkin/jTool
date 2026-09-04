package com.jtool.android.core.components.comments

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.Comment
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommentItem(
    comment: Comment,
    depth: Int,
    currentUserId: String,
    userNames: Map<String, String>,
    onEvent: (CommentEvent) -> Unit,
    onReplyRequest: (String) -> Unit,
    isOwner: (Comment) -> Boolean = { it.userId == currentUserId }
) {
    var isEditing by remember { mutableStateOf(false) }
    var editInput by remember { mutableStateOf(comment.content) }
    val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp, top = 8.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = userNames[comment.userId] ?: comment.userId,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (comment.isPending) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = dateFormat.format(Date(comment.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            if (comment.isEdited) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "(${Loc.Comments.edited})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            if (comment.isPending) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
            }
            Spacer(Modifier.weight(1f))
            
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Loc.Comments.reply) },
                        onClick = {
                            onReplyRequest(comment.id)
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(18.dp)) }
                    )
                    if (isOwner(comment) && !comment.isPending) {
                        DropdownMenuItem(
                            text = { Text(Loc.Comments.edit) },
                            onClick = {
                                isEditing = true
                                menuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text(Loc.General.delete, color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onEvent(CommentEvent.Delete(comment.id))
                                menuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }

        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = editInput,
                    onValueChange = { editInput = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = {
                    if (editInput.isNotBlank() && editInput != comment.content) {
                        onEvent(CommentEvent.Edit(comment.id, editInput))
                    }
                    isEditing = false
                }) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { isEditing = false; editInput = comment.content }) {
                    Icon(Icons.Default.Close, null)
                }
            }
        } else {
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (comment.isPending) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        comment.replies.forEach { reply ->
            CommentItem(
                comment = reply,
                depth = depth + 1,
                currentUserId = currentUserId,
                userNames = userNames,
                onEvent = onEvent,
                onReplyRequest = onReplyRequest,
                isOwner = isOwner
            )
        }
    }
}
