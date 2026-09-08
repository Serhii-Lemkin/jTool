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
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.features.settings.groups.components.create_group_sheet.CreateGroupSheet
import com.jtool.android.features.settings.groups.components.delete_group_confirm_sheet.DeleteGroupConfirmSheet
import com.jtool.android.features.settings.groups.components.edit_group_name_sheet.EditGroupNameSheet
import com.jtool.android.features.settings.groups.components.group_created_sheet.GroupCreatedSheet
import com.jtool.android.features.settings.groups.components.group_member_item_sheet.GroupMemberItem
import com.jtool.android.features.settings.groups.components.group_selection_sheet.GroupSelectionSheet
import com.jtool.android.features.settings.groups.components.invite_sheet.InviteSheet
import com.jtool.android.features.settings.groups.components.join_group_sheet.JoinGroupSheet
import com.jtool.android.features.settings.groups.components.leave_group_sheet.LeaveGroupSheet
import com.jtool.android.features.settings.groups.components.join_request_sent_sheet.JoinRequestSentSheet
import com.jtool.android.features.settings.groups.components.groups_info_sheet.GroupsInfoSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    dataManager: DataManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onGroupChanged: (String?) -> Unit,
    onGroupsUpdated: (Map<String, GroupInfo>) -> Unit,
    viewModel: GroupsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GroupsViewModel(dataManager, settingsManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val groupMemberIds by remember(uiState.currentGroupId) {
        if (uiState.currentGroupId != null) dataManager.groupMembersFlow(uiState.currentGroupId!!)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val globalUserNames by dataManager.userNamesFlow().collectAsState(initial = emptyMap())

    var groupSearchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    val allGroupIds = uiState.groups.keys.toList()
    val hasAnyGroups = allGroupIds.isNotEmpty() || uiState.currentGroupId != null
    
    var showGroupSelectionSheet by remember { mutableStateOf(false) }
    var showLeaveConfirmSheet by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }
    var showEditGroupNameDialog by remember { mutableStateOf(false) }
    var showGroupsInfoSheet by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    var showCreateGroupSheet by remember { mutableStateOf(false) }
    var showJoinGroupSheet by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.currentGroupId, uiState.groups) {
        onGroupChanged(uiState.currentGroupId)
        onGroupsUpdated(uiState.groups)
    }

    LaunchedEffect(uiState.message, uiState.error) {
        val msg = uiState.message ?: uiState.error
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(GroupsEvent.DismissMessage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GenericTitle(
                title = Loc.Settings.groups,
                onBack = onBack,
                isLoading = uiState.isLoading,
                onRefresh = { viewModel.onEvent(GroupsEvent.Refresh) },
                isSearchVisible = isSearching,
                searchQuery = groupSearchQuery,
                onSearchQueryChange = { groupSearchQuery = it },
                onSearchClose = { isSearching = false; groupSearchQuery = "" },
                actions = {
                    if (!isSearching) {
                        TitleActionIcon(icon = Icons.Default.Info, onClick = { showGroupsInfoSheet = true })
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val activeGroup = uiState.groups[uiState.currentGroupId]
            val isPrivate = activeGroup?.isPrivate == true
            val activeName = if (isPrivate && activeGroup.name.isEmpty()) Loc.Settings.privateGroup else activeGroup?.name ?: uiState.currentGroupId ?: Loc.Settings.noneSelected

            if (allGroupIds.isNotEmpty()) {
                SectionHeader(Loc.Settings.activeGroup)
                ActionListItem(
                    headline = activeName,
                    leadingContent = { 
                        Icon(
                            imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Group, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary 
                        ) 
                    },
                    trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                    onClick = { showGroupSelectionSheet = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                if (uiState.currentGroupId != null) {
                    ActionListItem(
                        headline = Loc.Settings.editGroupName,
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { 
                            newGroupName = activeName
                            showEditGroupNameDialog = true 
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            } else if (!hasAnyGroups && !uiState.isLoading) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))) {
                    Text(text = Loc.Settings.noGroupsMsg, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }

            if (uiState.currentGroupId != null && !uiState.isLoading && !isPrivate) {
                SectionHeader("Group Management")
                
                ActionListItem(
                    headline = Loc.Settings.groupInvite,
                    supportingText = "Invite members via email",
                    leadingContent = { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { showInviteSheet = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                ActionListItem(
                    headline = Loc.Settings.copyId,
                    supportingText = uiState.currentGroupId,
                    leadingContent = { Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { clipboardManager.setText(AnnotatedString(uiState.currentGroupId!!)) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                ActionListItem(
                    headline = Loc.Settings.unlinkGroup,
                    supportingText = "Remove this group from your account",
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
                    headlineColor = MaterialTheme.colorScheme.error,
                    onClick = { showLeaveConfirmSheet = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(24.dp))
            }

            SectionHeader(Loc.Settings.addNewGroup)
            ActionListItem(
                headline = Loc.Settings.createNewGroup,
                leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { showCreateGroupSheet = true },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            ActionListItem(
                headline = Loc.Settings.joinGroup,
                leadingContent = { Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { showJoinGroupSheet = true },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (uiState.currentGroupId != null && groupMemberIds.isNotEmpty() && !uiState.isLoading && !isPrivate) {
                Spacer(Modifier.height(32.dp))
                SectionHeader(Loc.Settings.groupMembers)
                groupMemberIds.forEach { uid ->
                    val name = globalUserNames[uid] ?: Loc.General.unknown
                    val isOwner = activeGroup?.ownerUid == uid
                    GroupMemberItem(name = name, isOwner = isOwner)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showGroupsInfoSheet) {
        GroupsInfoSheet(onDismiss = { showGroupsInfoSheet = false })
    }

    if (showGroupSelectionSheet) {
        GroupSelectionSheet(
            groups = uiState.groups,
            currentGroupId = uiState.currentGroupId,
            onGroupSelected = { viewModel.onEvent(GroupsEvent.SwitchGroup(it)) },
            onDismiss = { showGroupSelectionSheet = false }
        )
    }

    if (showCreateGroupSheet) {
        CreateGroupSheet(
            onDismiss = { showCreateGroupSheet = false },
            onConfirm = { viewModel.onEvent(GroupsEvent.CreateGroup(it)); showCreateGroupSheet = false }
        )
    }
    
    if (showJoinGroupSheet) {
        JoinGroupSheet(
            onDismiss = { showJoinGroupSheet = false },
            onConfirm = { viewModel.onEvent(GroupsEvent.JoinGroup(it)); showJoinGroupSheet = false }
        )
    }

    if (showLeaveConfirmSheet) {
        LeaveGroupSheet(
            isLastMember = groupMemberIds.size <= 1,
            onDismiss = { showLeaveConfirmSheet = false },
            onConfirm = { viewModel.onEvent(GroupsEvent.LeaveGroup(uiState.currentGroupId!!)); showLeaveConfirmSheet = false }
        )
    }

    if (showInviteSheet) {
        InviteSheet(
            onDismiss = { showInviteSheet = false },
            onConfirm = { viewModel.onEvent(GroupsEvent.SendInvite(it, uiState.currentGroupId!!)); showInviteSheet = false }
        )
    }

    if (showEditGroupNameDialog) {
        EditGroupNameSheet(initialName = newGroupName, onDismiss = { showEditGroupNameDialog = false }, onConfirm = { viewModel.onEvent(GroupsEvent.UpdateGroupName(uiState.currentGroupId!!, it)); showEditGroupNameDialog = false })
    }
    
    if (uiState.lastCreatedGroupId != null) {
        GroupCreatedSheet(groupId = uiState.lastCreatedGroupId!!, onDismiss = { viewModel.onEvent(GroupsEvent.DismissCreatedDialog) }, onCopy = { clipboardManager.setText(AnnotatedString(it)) })
    }

    if (uiState.joinRequestResult != null) {
        JoinRequestSentSheet(result = uiState.joinRequestResult!!, onDismiss = { viewModel.onEvent(GroupsEvent.DismissJoinRequestSentSheet) })
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
