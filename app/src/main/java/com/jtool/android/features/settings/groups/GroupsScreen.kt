package com.jtool.android.features.settings.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.features.settings.groups.components.*

@Composable
fun GroupsScreen(
    groupSyncManager: GroupSyncManager,
    settingsManager: SettingsManager,
    onGroupChanged: (String?) -> Unit,
    onGroupsUpdated: (Map<String, GroupInfo>) -> Unit,
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
        else kotlinx.coroutines.flow.flowOf(emptyList())
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

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.currentGroupId, uiState.groups) {
        onGroupChanged(uiState.currentGroupId)
        onGroupsUpdated(uiState.groups)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Group Selection
        if (allGroupIds.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Loc.settings_activeGroup, style = MaterialTheme.typography.labelSmall)
                            val activeName = uiState.groups[uiState.currentGroupId]?.name ?: uiState.currentGroupId ?: Loc.settings_noneSelected
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
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = groupSearchQuery,
                        onValueChange = { groupSearchQuery = it },
                        label = { Text(Loc.general_searchGroups) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    val filteredGroups = allGroupIds.filter { 
                        uiState.groups[it]?.name?.contains(groupSearchQuery, ignoreCase = true) == true || it.contains(groupSearchQuery, ignoreCase = true) 
                    }
                    filteredGroups.forEach { id ->
                        DropdownMenuItem(
                            text = { Text(uiState.groups[id]?.name ?: id) },
                            onClick = { viewModel.onEvent(GroupsEvent.SwitchGroup(id)); expanded = false },
                            trailingIcon = { if (id == uiState.currentGroupId) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                }
            }
        } else if (!hasAnyGroups && !uiState.isLoading) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))) {
                Text(text = Loc.settings_noGroupsMsg, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        val activeGroupId = uiState.currentGroupId
        if (activeGroupId != null && !uiState.isLoading) {
            Text(Loc.settings_linkedGroupId, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.onEvent(GroupsEvent.ManualSync) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CloudSync, null); Spacer(Modifier.width(12.dp)); Text(Loc.settings_manualSync)
                }
                Button(onClick = { showInviteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text(Loc.settings_groupInvite)
                }
                Spacer(Modifier.height(4.dp)); Text(Loc.settings_shareIdDesc, style = MaterialTheme.typography.bodySmall)
                Button(onClick = { clipboardManager.setText(AnnotatedString(activeGroupId)) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text(Loc.settings_copyId)
                }
                Button(
                    onClick = { if (groupMemberIds.size <= 1) showLastMemberLeaveConfirm = activeGroupId else showUnlinkConfirm = activeGroupId },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Icon(Icons.AutoMirrored.Filled.ExitToApp, null); Spacer(Modifier.width(8.dp)); Text(Loc.settings_unlinkGroup) }
            }
            Spacer(Modifier.height(24.dp)); HorizontalDivider(); Spacer(Modifier.height(24.dp))
        }

        if (!uiState.isLoading) {
            Text(Loc.settings_addNewGroup, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showCreateGroupDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(Loc.settings_createNewGroup)
                }
                Button(onClick = { showJoinGroupDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.GroupAdd, null); Spacer(Modifier.width(8.dp)); Text(Loc.settings_joinGroup)
                }
            }
        }

        if (uiState.currentGroupId != null && groupMemberIds.isNotEmpty() && !uiState.isLoading) {
            Spacer(Modifier.height(32.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))
            Text(Loc.settings_groupMembers, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            groupMemberIds.forEach { uid ->
                val name = globalUserNames[uid] ?: Loc.general_unknown
                val isOwner = uiState.groups[uiState.currentGroupId]?.owner == uid
                GroupMemberItem(name = name, isOwner = isOwner)
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    // Dialogs
    if (showCreateGroupDialog) {
        CreateGroupDialog(onDismiss = { showCreateGroupDialog = false }, onConfirm = { viewModel.onEvent(GroupsEvent.CreateGroup(it)); showCreateGroupDialog = false })
    }
    if (showJoinGroupDialog) {
        JoinGroupDialog(onDismiss = { showJoinGroupDialog = false }, onConfirm = { viewModel.onEvent(GroupsEvent.JoinGroup(it)); showJoinGroupDialog = false })
    }
    if (showUnlinkConfirm != null) {
        UnlinkConfirmDialog(onDismiss = { showUnlinkConfirm = null }, onConfirm = { viewModel.onEvent(GroupsEvent.LeaveGroup(showUnlinkConfirm!!)); showUnlinkConfirm = null })
    }
    if (showLastMemberLeaveConfirm != null) {
        LastMemberLeaveDialog(onDismiss = { showLastMemberLeaveConfirm = null }, onConfirm = { viewModel.onEvent(GroupsEvent.LeaveGroup(showLastMemberLeaveConfirm!!)); showLastMemberLeaveConfirm = null })
    }
    if (showInviteDialog) {
        InviteDialog(onDismiss = { showInviteDialog = false }, onConfirm = { viewModel.onEvent(GroupsEvent.SendInvite(it, uiState.currentGroupId!!)); showInviteDialog = false })
    }
    if (showEditGroupNameDialog) {
        EditGroupNameDialog(initialName = newGroupName, onDismiss = { showEditGroupNameDialog = false }, onConfirm = { viewModel.onEvent(GroupsEvent.UpdateGroupName(uiState.currentGroupId!!, it)); showEditGroupNameDialog = false })
    }
    if (showDeleteGroupConfirm1 != null) {
        DeleteGroupConfirmDialog(step = 1, onDismiss = { showDeleteGroupConfirm1 = null }, onNext = { showDeleteGroupConfirm2 = showDeleteGroupConfirm1; showDeleteGroupConfirm1 = null }, onConfirm = {})
    }
    if (showDeleteGroupConfirm2 != null) {
        DeleteGroupConfirmDialog(step = 2, onDismiss = { showDeleteGroupConfirm2 = null }, onNext = {}, onConfirm = { viewModel.onEvent(GroupsEvent.DeleteGroup(showDeleteGroupConfirm2!!)); showDeleteGroupConfirm2 = null })
    }
    if (uiState.lastCreatedGroupId != null) {
        GroupCreatedDialog(groupId = uiState.lastCreatedGroupId!!, onDismiss = { viewModel.onEvent(GroupsEvent.DismissCreatedDialog) }, onCopy = { clipboardManager.setText(AnnotatedString(it)) })
    }
}
