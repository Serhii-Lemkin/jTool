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
import androidx.lifecycle.lifecycleScope
import com.example.jtool.GroupSyncManager
import com.example.jtool.Loc
import com.example.jtool.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun GroupsScreen(
    groupSyncManager: GroupSyncManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onGroupChanged: (String?) -> Unit // Added this
) {
    var groupNameInput by remember { mutableStateOf("") }
    var joinGroupId by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }
    var currentGroupId by remember { mutableStateOf(settingsManager.getCurrentGroupId()) }
    var allGroupIds by remember { mutableStateOf(settingsManager.getGroupIds()) }
    val groupNames by remember(allGroupIds) { 
        groupSyncManager.groupNamesFlow(allGroupIds) 
    }.collectAsState(initial = emptyMap())
    
    var expanded by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var showUnlinkConfirm by remember { mutableStateOf<String?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    fun refreshState() {
        currentGroupId = settingsManager.getCurrentGroupId()
        allGroupIds = settingsManager.getGroupIds()
        if(allGroupIds.count() == 0) {
            scope.launch {
                allGroupIds = groupSyncManager.getUserGroups()
            }
        }

        android.util.Log.d("GroupSync", "GroupsScreen state refreshed. Count: ${allGroupIds.size}, Current: $currentGroupId")
    }

    // Automatically refresh groups when entering the screen
    LaunchedEffect(Unit) {
        android.util.Log.d("GroupSync", "GroupsScreen Entered. Fetching settings...")
        groupSyncManager.fetchUserSettings()
        refreshState()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Loc.groups, fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

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

        // Group Selection Dropdown
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
                            Text(groupNames[currentGroupId] ?: currentGroupId ?: Loc.noneSelected, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                            text = { Text(groupNames[id] ?: id) },
                            onClick = {
                                scope.launch {
                                    groupSyncManager.switchGroup(id)
                                    onGroupChanged(id) // Notify main loop to restart listener
                                    expanded = false
                                    refreshState()
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
        }

        val activeGroupId = currentGroupId
        if (activeGroupId != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(Loc.linkedGroupId, style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeGroupId.take(8) + "...", 
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
                    
                    Spacer(Modifier.height(8.dp))
                    Text(Loc.shareIdDesc, fontSize = 11.sp)
                    
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
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { 
                scope.launch { 
                    val result = groupSyncManager.createGroup(groupNameInput)
                    if (result != null) {
                        onGroupChanged(result)
                        android.util.Log.d("GroupSync", "UI success: $result")
                    } else {
                        android.util.Log.e("GroupSync", "UI failure")
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
            modifier = Modifier.fillMaxWidth()
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
                    settingsManager.setCurrentGroupId(null)
                    settingsManager.removeGroupId(gid)
                    onGroupChanged(null) // Notify main loop
                    showUnlinkConfirm = null
                    refreshState()
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
                    Text(Loc.enterEmail, fontSize = 12.sp)
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
}

@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}
