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
        title = { Text(Loc.createNewGroup) },
        text = {
            OutlinedTextField(
                value = groupNameInput,
                onValueChange = { groupNameInput = it },
                label = { Text(Loc.groupName) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(groupNameInput) },
                enabled = groupNameInput.isNotBlank()
            ) { Text(Loc.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Loc.cancel) }
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
        title = { Text(Loc.joinGroup) },
        text = {
            OutlinedTextField(
                value = joinGroupId,
                onValueChange = { joinGroupId = it },
                label = { Text(Loc.groupIdToJoin) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(joinGroupId) },
                enabled = joinGroupId.isNotBlank()
            ) { Text(Loc.joinGroup) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Loc.cancel) }
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
        title = { Text(Loc.confirmUnlinkTitle) },
        text = { Text(Loc.confirmUnlinkMsg) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(Loc.unlinkGroup, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.cancel)
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
        title = { Text(Loc.deleteGroupTitle) },
        text = { Text(Loc.lastMemberLeaveMsg) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(Loc.unlinkGroup)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.cancel)
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
        title = { Text(Loc.groupInvite) },
        text = {
            Column {
                Text(Loc.enterEmail, style = MaterialTheme.typography.bodySmall)
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
                Text(Loc.sendRequest)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.cancel)
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
        title = { Text(Loc.editGroupName) },
        text = {
            OutlinedTextField(
                value = newGroupName,
                onValueChange = { newGroupName = it },
                label = { Text(Loc.groupName) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newGroupName) },
                enabled = newGroupName.isNotBlank()
            ) {
                Text(Loc.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.cancel)
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
            title = { Text(Loc.deleteGroup) },
            text = { Text(Loc.deleteGroupPermanently) },
            confirmButton = {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(Loc.cancel)
                }
            }
        )
    } else if (step == 2) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(Loc.areYouSure) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(Loc.cancel)
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
        title = { Text(Loc.groupCreated) },
        text = {
            Column {
                Text(Loc.groupIdLabel.format(groupId))
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onCopy(groupId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.copyId)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.close)
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
                        label = { Text(Loc.ownerLabel, fontSize = 10.sp) },
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
