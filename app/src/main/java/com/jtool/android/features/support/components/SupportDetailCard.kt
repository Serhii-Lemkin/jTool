package com.jtool.android.features.support.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import com.jtool.android.core.components.comments.CommentSection
import com.jtool.android.core.components.comments.CommentEvent

@Composable
fun SupportDetailCard(
    request: SupportRequest,
    isAdmin: Boolean,
    currentUserId: String,
    onStatusUpdate: (SupportStatusEnum) -> Unit,
    onTypeUpdate: (SupportTypeEnum) -> Unit,
    onDelete: () -> Unit,
    onCommentEvent: (CommentEvent) -> Unit,
    userNames: Map<String, String> = emptyMap(),
    isOwner: (com.jtool.android.core.models.common.Comment) -> Boolean = { it.userId == currentUserId }
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isAdmin) {
                    Box {
                        val displayType = request.pendingType ?: request.type
                        val isPending = request.pendingType != null
                        TextButton(
                            onClick = { typeMenuExpanded = true },
                            contentPadding = PaddingValues(0.dp),
                            enabled = !isPending
                        ) {
                            Text(
                                text = when (displayType) {
                                    SupportTypeEnum.BUG -> Loc.Support.type_bug
                                    SupportTypeEnum.CRASH -> Loc.Support.type_crash
                                    SupportTypeEnum.FEATURE -> Loc.Support.type_feature
                                    SupportTypeEnum.CHANGE -> Loc.Support.type_change
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isPending) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
                            )
                            if (isPending) {
                                Spacer(Modifier.width(4.dp))
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                            } else {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            SupportTypeEnum.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (type) {
                                                SupportTypeEnum.BUG -> Loc.Support.type_bug
                                                SupportTypeEnum.CRASH -> Loc.Support.type_crash
                                                SupportTypeEnum.FEATURE -> Loc.Support.type_feature
                                                SupportTypeEnum.CHANGE -> Loc.Support.type_change
                                            }
                                        )
                                    },
                                    onClick = {
                                        onTypeUpdate(type)
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    val displayType = request.pendingType ?: request.type
                    Text(
                        text = when (displayType) {
                            SupportTypeEnum.BUG -> Loc.Support.type_bug
                            SupportTypeEnum.CRASH -> Loc.Support.type_crash
                            SupportTypeEnum.FEATURE -> Loc.Support.type_feature
                            SupportTypeEnum.CHANGE -> Loc.Support.type_change
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (request.pendingType != null) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin) {
                    Box {
                        val displayStatus = request.pendingStatus ?: request.status
                        val isPending = request.pendingStatus != null
                        AssistChip(
                            onClick = { statusMenuExpanded = true },
                            label = {
                                Text(
                                    when (displayStatus) {
                                        SupportStatusEnum.OPENED -> Loc.Support.status_opened
                                        SupportStatusEnum.IN_PROGRESS -> Loc.Support.status_in_progress
                                        SupportStatusEnum.CLOSED -> Loc.Support.status_closed
                                        SupportStatusEnum.CUT -> Loc.Support.status_cut
                                    }
                                )
                            },
                            trailingIcon = { 
                                if (isPending) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp)
                                } else {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            enabled = !isPending
                        )
                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            SupportStatusEnum.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (status) {
                                                SupportStatusEnum.OPENED -> Loc.Support.status_opened
                                                SupportStatusEnum.IN_PROGRESS -> Loc.Support.status_in_progress
                                                SupportStatusEnum.CLOSED -> Loc.Support.status_closed
                                                SupportStatusEnum.CUT -> Loc.Support.status_cut
                                            }
                                        )
                                    },
                                    onClick = {
                                        onStatusUpdate(status)
                                        statusMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    StatusBadge(
                        status = request.pendingStatus ?: request.status,
                        isPending = request.pendingStatus != null
                    )
                }

                Box {
                    IconButton(onClick = { actionMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = actionMenuExpanded, onDismissRequest = { actionMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(Loc.Settings.copyId) },
                            onClick = {
                                clipboardManager.setText(AnnotatedString(request.id))
                                Toast.makeText(context, "ID Copied", Toast.LENGTH_SHORT).show()
                                actionMenuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                        )
                        if (isAdmin || request.status == SupportStatusEnum.CUT) {
                            DropdownMenuItem(
                                text = { Text(Loc.General.delete, color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    actionMenuExpanded = false
                                    showDeleteConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(text = request.text, style = MaterialTheme.typography.bodyLarge)
        
        if (request.crashLog != null) {
            Spacer(Modifier.height(24.dp))
            Text(text = Loc.Support.crash_log, style = MaterialTheme.typography.titleSmall)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = request.crashLog,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        if (isAdmin || request.deviceInfo.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(text = Loc.Support.device_info, style = MaterialTheme.typography.titleSmall)
            request.deviceInfo.forEach { (key, value) ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = "$key: ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = value, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        Spacer(Modifier.height(16.dp))
        
        CommentSection(
            comments = request.comments,
            currentUserId = currentUserId,
            userNames = userNames,
            onEvent = onCommentEvent,
            initiallyExpanded = true,
            isOwner = isOwner
        )
        
        Spacer(Modifier.height(32.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(Loc.General.confirmDeleteTitle) },
            text = { Text(Loc.General.confirmDeleteMsg) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text(Loc.General.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(Loc.General.cancel)
                }
            }
        )
    }
}
