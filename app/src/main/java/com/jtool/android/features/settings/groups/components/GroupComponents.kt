package com.jtool.android.features.settings.groups.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.GroupInfo

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupNameInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.settings_createNewGroup) },
        text = {
            OutlinedTextField(
                value = groupNameInput,
                onValueChange = { groupNameInput = it },
                label = { Text(Loc.settings_groupName) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(groupNameInput) },
                enabled = groupNameInput.isNotBlank()
            ) { Text(Loc.general_save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Loc.general_cancel) }
        }
    )
}

@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var joinGroupId by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.settings_joinGroup) },
        text = {
            OutlinedTextField(
                value = joinGroupId,
                onValueChange = { joinGroupId = it },
                label = { Text(Loc.settings_groupIdToJoin) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(joinGroupId) },
                enabled = joinGroupId.isNotBlank()
            ) { Text(Loc.settings_joinGroup) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Loc.general_cancel) }
        }
    )
}

@Composable
fun UnlinkConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.settings_confirmUnlinkTitle) },
        text = { Text(Loc.settings_confirmUnlinkMsg) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(Loc.settings_unlinkGroup, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.general_cancel)
            }
        }
    )
}

@Composable
fun LastMemberLeaveDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.settings_deleteGroupTitle) },
        text = { Text(Loc.settings_lastMemberLeaveMsg) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(Loc.settings_unlinkGroup)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.general_cancel)
            }
        }
    )
}

@Composable
fun InviteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inviteEmail by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.settings_groupInvite) },
        text = {
            Column {
                Text(Loc.settings_enterEmail, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = inviteEmail,
                    onValueChange = { inviteEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inviteEmail) },
                enabled = inviteEmail.contains("@")
            ) {
                Text(Loc.support_submit)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.general_cancel)
            }
        }
    )
}

@Composable
fun EditGroupNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newGroupName by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.settings_editGroupName) },
        text = {
            OutlinedTextField(
                value = newGroupName,
                onValueChange = { newGroupName = it },
                label = { Text(Loc.settings_groupName) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newGroupName) },
                enabled = newGroupName.isNotBlank()
            ) {
                Text(Loc.general_save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.general_cancel)
            }
        }
    )
}

@Composable
fun DeleteGroupConfirmDialog(
    step: Int,
    onDismiss: () -> Unit,
    onNext: () -> Unit,
    onConfirm: () -> Unit
) {
    if (step == 1) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(Loc.admin_deleteGroup) },
            text = { Text(Loc.admin_deleteGroupPermanently) },
            confirmButton = {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.general_delete)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(Loc.general_cancel)
                }
            }
        )
    } else if (step == 2) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(Loc.general_areYouSure) },
            text = { Text(Loc.general_confirmDeleteMsg) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.general_delete)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }
}

@Composable
fun GroupCreatedDialog(
    groupId: String,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.settings_groupCreated) },
        text = {
            Column {
                Text(Loc.settings_groupIdLabel.format(groupId))
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onCopy(groupId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.settings_copyId)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.general_close)
            }
        }
    )
}

@Composable
fun GroupMemberItem(
    name: String,
    isOwner: Boolean
) {
    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name)
                if (isOwner) {
                    Spacer(Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = { },
                        label = { Text(Loc.settings_ownerLabel, fontSize = 10.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = null
                    )
                }
            }
        },
        leadingContent = { Icon(Icons.Default.Person, null) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}
