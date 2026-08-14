package com.jtool.android.features.settings.groups

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.JGroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import androidx.compose.material.icons.automirrored.filled.ExitToApp

@Composable
fun GroupsScreen(
    groupSyncManager: GroupSyncManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onGroupChanged: (String?) -> Unit,
    onGroupsUpdated: (Map<String, JGroupInfo>) -> Unit,
    viewModel: GroupsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GroupsViewModel(groupSyncManager, settingsManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val groupMemberIds by remember(uiState.currentGroupId) {
        if (uiState.currentGroupId != null) groupSyncManager.groupMembersFlow(uiState.currentGroupId!!)
        else kotlinx.coroutines.flow.flowOf(emptyList<String>())
    }.collectAsState(initial = emptyList())

    val globalUserNames by groupSyncManager.userNamesFlow().collectAsState(initial = emptyMap())

    var groupSearchQuery by remember { mutableStateOf("") }
    
    val allGroupIds = uiState.groups.keys.toList()
    val hasAnyGroups = allGroupIds.isNotEmpty() || uiState.currentGroupId != null
    
    var expanded by remember { mutableStateOf(false) }
    var showUnlinkConfirm by remember { mutableStateOf<String?>(null) }
    var showLastMemberLeaveConfirm by remember { mutableStateOf<String?>(null) }
    var showDeleteGroupConfirm1 by remember { mutableStateOf<String?>(null) }
    var showDeleteGroupConfirm2 by remember { mutableStateOf<String?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showEditGroupNameDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var groupNameInput by remember { mutableStateOf("") }
    var joinGroupId by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.currentGroupId, uiState.groups) {
        onGroupChanged(uiState.currentGroupId)
        onGroupsUpdated(uiState.groups)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

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
                            val activeName = uiState.groups[uiState.currentGroupId]?.name ?: uiState.currentGroupId ?: Loc.noneSelected
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(activeName, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                if (uiState.currentGroupId != null) {
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
                    modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 400.dp)
                ) {
                    OutlinedTextField(
                        value = groupSearchQuery,
                        onValueChange = { groupSearchQuery = it },
                        label = { Text(Loc.searchGroups) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    
                    val filteredGroups = allGroupIds.filter { 
                        uiState.groups[it]?.name?.contains(groupSearchQuery, ignoreCase = true) == true || 
                        it.contains(groupSearchQuery, ignoreCase = true) 
                    }

                    filteredGroups.forEach { id ->
                        DropdownMenuItem(
                            text = { Text(uiState.groups[id]?.name ?: id) },
                            onClick = {
                                viewModel.onEvent(GroupsEvent.SwitchGroup(id))
                                expanded = false
                            },
                            trailingIcon = {
                                if (id == uiState.currentGroupId) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                    if (filteredGroups.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(Loc.t("No groups found", "Груп не знайдено")) },
                            onClick = { },
                            enabled = false
                        )
                    }
                }
            }
        } else if (!hasAnyGroups && !uiState.isLoading) {
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

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        val activeGroupId = uiState.currentGroupId
        if (activeGroupId != null && !uiState.isLoading) {
            Text(Loc.linkedGroupId, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.onEvent(GroupsEvent.ManualSync) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudSync, null)
                    Spacer(Modifier.width(12.dp))
                    Text(Loc.manualSync)
                }

                Button(
                    onClick = { showInviteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.groupInvite)
                }

                Spacer(Modifier.height(4.dp))
                Text(Loc.shareIdDesc, style = MaterialTheme.typography.bodySmall)

                Button(
                    onClick = { clipboardManager.setText(AnnotatedString(activeGroupId)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.copyId)
                }

                Button(
                    onClick = { 
                        if (groupMemberIds.size <= 1) {
                            showLastMemberLeaveConfirm = activeGroupId
                        } else {
                            showUnlinkConfirm = activeGroupId 
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.unlinkGroup)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
        }

        // Create/Join section
        if (!uiState.isLoading) {
            Text(Loc.addNewGroup, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showCreateGroupDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.createNewGroup)
                }
                
                Button(
                    onClick = { showJoinGroupDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.GroupAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.joinGroup)
                }
            }
        }

        // Group Members Section
        if (uiState.currentGroupId != null && groupMemberIds.isNotEmpty() && !uiState.isLoading) {
            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(Loc.t("Group Members", "Учасники групи"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            
            groupMemberIds.forEach { uid ->
                val name = globalUserNames[uid] ?: Loc.t("Unknown", "Невідомо")
                val isOwner = uiState.groups[uiState.currentGroupId]?.owner == uid
                
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
        }
        
        Spacer(Modifier.height(32.dp))
    }

    // --- Dialogs ---

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
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
                    onClick = {
                        viewModel.onEvent(GroupsEvent.CreateGroup(groupNameInput))
                        groupNameInput = ""
                        showCreateGroupDialog = false
                    },
                    enabled = groupNameInput.isNotBlank()
                ) { Text(Loc.save) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text(Loc.cancel) }
            }
        )
    }

    if (showJoinGroupDialog) {
        AlertDialog(
            onDismissRequest = { showJoinGroupDialog = false },
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
                    onClick = {
                        viewModel.onEvent(GroupsEvent.JoinGroup(joinGroupId))
                        joinGroupId = ""
                        showJoinGroupDialog = false
                    },
                    enabled = joinGroupId.isNotBlank()
                ) { Text(Loc.joinGroup) }
            },
            dismissButton = {
                TextButton(onClick = { showJoinGroupDialog = false }) { Text(Loc.cancel) }
            }
        )
    }

    if (showUnlinkConfirm != null) {
        AlertDialog(
            onDismissRequest = { showUnlinkConfirm = null },
            title = { Text(Loc.confirmUnlinkTitle) },
            text = { Text(Loc.confirmUnlinkMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(GroupsEvent.LeaveGroup(showUnlinkConfirm!!))
                    showUnlinkConfirm = null
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

    if (showLastMemberLeaveConfirm != null) {
        AlertDialog(
            onDismissRequest = { showLastMemberLeaveConfirm = null },
            title = { Text(Loc.t("Delete Group?", "Видалити групу?")) },
            text = { Text(Loc.t("Because you are the last person to leave the group, on leave the group will be permanently deleted. Are you sure?", 
                               "Оскільки ви остання людина, яка залишає групу, після вашого виходу вона буде остаточно видалена. Ви впевнені?")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(GroupsEvent.LeaveGroup(showLastMemberLeaveConfirm!!))
                        showLastMemberLeaveConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLastMemberLeaveConfirm = null }) {
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
                        val gid = uiState.currentGroupId
                        if (gid != null && inviteEmail.isNotBlank()) {
                            viewModel.onEvent(GroupsEvent.SendInvite(inviteEmail, gid))
                            inviteEmail = ""
                            showInviteDialog = false
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
                        val gid = uiState.currentGroupId
                        if (gid != null && newGroupName.isNotBlank()) {
                            viewModel.onEvent(GroupsEvent.UpdateGroupName(gid, newGroupName))
                            showEditGroupNameDialog = false
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

    if (showDeleteGroupConfirm1 != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm1 = null },
            title = { Text(Loc.deleteGroup) },
            text = { Text(Loc.deleteGroupPermanently) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteGroupConfirm2 = showDeleteGroupConfirm1
                        showDeleteGroupConfirm1 = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirm1 = null }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (showDeleteGroupConfirm2 != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm2 = null },
            title = { Text(Loc.areYouSure) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(GroupsEvent.DeleteGroup(showDeleteGroupConfirm2!!))
                        showDeleteGroupConfirm2 = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirm2 = null }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (uiState.lastCreatedGroupId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(GroupsEvent.DismissCreatedDialog) },
            title = { Text(Loc.t("Group Created!", "Групу створено!")) },
            text = {
                Column {
                    Text(Loc.t("Group ID: ${uiState.lastCreatedGroupId}", "ID групи: ${uiState.lastCreatedGroupId}"))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { clipboardManager.setText(AnnotatedString(uiState.lastCreatedGroupId!!)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                        Spacer(Modifier.width(8.dp))
                        Text(Loc.copyId)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(GroupsEvent.DismissCreatedDialog) }) {
                    Text(Loc.t("Close", "Закрити"))
                }
            }
        )
    }
}
