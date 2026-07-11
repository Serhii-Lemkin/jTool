package com.example.jtool.settingsSections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jtool.GroupSyncManager
import com.example.jtool.Loc
import com.example.jtool.SettingsManager
import com.example.jtool.JGroupInfo
import kotlinx.coroutines.launch

@Composable
fun GroupsScreen(
    groupSyncManager: GroupSyncManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onGroupChanged: (String?) -> Unit,
    onGroupsUpdated: (Map<String, JGroupInfo>) -> Unit,
    currentGroupId: String?,
    groups: Map<String, JGroupInfo>
) {
    var groupNameInput by remember { mutableStateOf("") }
    var joinGroupId by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }
    
    val allGroupIds = groups.keys.toList()
    val hasAnyGroups = allGroupIds.isNotEmpty() || currentGroupId != null
    
    var expanded by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var showUnlinkConfirm by remember { mutableStateOf<String?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showEditGroupNameDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    fun refreshState() {
        onGroupsUpdated(settingsManager.getGroups())
    }

    LaunchedEffect(Unit) {
        groupSyncManager.fetchUserSettings()
        refreshState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Manual Sync Merge Button
        if (currentGroupId != null) {
            Button(
                onClick = {
                    isSyncing = true
                    scope.launch {
                        groupSyncManager.manualSyncMerge()
                        isSyncing = false
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(Loc.syncing)
                } else {
                    Icon(Icons.Default.CloudSync, null)
                    Spacer(Modifier.width(12.dp))
                    Text(Loc.manualSync)
                }
            }
        }

        // Group Selection Card
        if (allGroupIds.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                OutlinedCard(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Loc.activeGroup, style = MaterialTheme.typography.labelSmall)
                            val activeName = groups[currentGroupId]?.name ?: currentGroupId ?: Loc.noneSelected
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(activeName, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                if (currentGroupId != null) {
                                    IconButton(
                                        onClick = { 
                                            newGroupName = activeName
                                            showEditGroupNameDialog = true 
                                            expanded = false
                                        },
                                        modifier = Modifier.size(24.dp).padding(start = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    allGroupIds.forEach { id ->
                        DropdownMenuItem(
                            text = { Text(groups[id]?.name ?: id) },
                            onClick = {
                                scope.launch {
                                    groupSyncManager.switchGroup(id)
                                    onGroupChanged(id)
                                    onGroupsUpdated(settingsManager.getGroups())
                                    expanded = false
                                }
                            },
                            trailingIcon = {
                                if (id == currentGroupId) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            }
        } else if (!hasAnyGroups) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
            ) {
                Text(
                    text = Loc.noGroupsMsg,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        val activeGroupId = currentGroupId
        if (activeGroupId != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(Loc.linkedGroupId, style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeGroupId.take(8) + "...", 
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(activeGroupId)) }) {
                            Icon(Icons.Default.ContentCopy, Loc.copyId)
                        }
                        IconButton(onClick = { showUnlinkConfirm = activeGroupId }) {
                            Icon(Icons.Default.LinkOff, Loc.unlinkGroup, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Text(Loc.shareIdDesc, style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showInviteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PersonAdd, null)
                        Spacer(Modifier.width(8.dp))
                        Text(Loc.groupInvite)
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
        }

        // Create/Join section
        Text(Loc.addNewGroup, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = groupNameInput,
            onValueChange = { groupNameInput = it },
            label = { Text(Loc.groupName) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = { 
                scope.launch { 
                    val result = groupSyncManager.createGroup(groupNameInput)
                    if (result != null) {
                        onGroupChanged(result)
                    }
                    groupNameInput = ""
                    refreshState()
                } 
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = groupNameInput.isNotBlank()
        ) {
            Text(Loc.createNewGroup)
        }

        Spacer(Modifier.height(16.dp))
        Text(Loc.or, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = joinGroupId,
            onValueChange = { joinGroupId = it },
            label = { Text(Loc.groupIdToJoin) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = { 
                scope.launch { 
                    if (groupSyncManager.joinGroup(joinGroupId)) {
                        onGroupChanged(joinGroupId)
                    }
                    joinGroupId = ""
                    refreshState()
                } 
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = joinGroupId.isNotBlank()
        ) {
            Text(Loc.joinGroup)
        }
    }

    // --- Dialogs ---

    if (showUnlinkConfirm != null) {
        AlertDialog(
            onDismissRequest = { showUnlinkConfirm = null },
            title = { Text(Loc.confirmUnlinkTitle) },
            text = { Text(Loc.confirmUnlinkMsg) },
            confirmButton = {
                TextButton(onClick = {
                    val gid = showUnlinkConfirm!!
                    scope.launch {
                        groupSyncManager.leaveGroup(gid)
                        onGroupChanged(settingsManager.getCurrentGroupId())
                        showUnlinkConfirm = null
                        refreshState()
                    }
                }) {
                    Text(Loc.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkConfirm = null }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
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
                    onClick = {
                        val gid = currentGroupId
                        if (gid != null && inviteEmail.isNotBlank()) {
                            scope.launch {
                                groupSyncManager.sendInvite(inviteEmail, gid)
                                inviteEmail = ""
                                showInviteDialog = false
                            }
                        }
                    },
                    enabled = inviteEmail.contains("@")
                ) {
                    Text(Loc.sendRequest)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (showEditGroupNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditGroupNameDialog = false },
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
                    onClick = {
                        val gid = currentGroupId
                        if (gid != null && newGroupName.isNotBlank()) {
                            scope.launch {
                                groupSyncManager.updateGroupName(gid, newGroupName)
                                showEditGroupNameDialog = false
                                refreshState()
                            }
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text(Loc.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGroupNameDialog = false }) {
                    Text(Loc.cancel)
                }
            }
        )
    }
}
