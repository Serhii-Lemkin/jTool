package com.jtool.android.features.admin

import android.util.Log
import com.jtool.android.core.models.admin.enums.AdminSubScreenEnum
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.features.admin.components.delete_user_confirm_sheet.DeleteUserConfirmSheet
import com.jtool.android.features.admin.components.notification_creator_sheet.NotificationCreatorSheet
import com.jtool.android.features.settings.groups.components.delete_group_confirm_sheet.DeleteGroupConfirmSheet
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    dataManager: DataManager,
    onBack: () -> Unit,
    viewModel: AdminViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AdminViewModel(dataManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var isSearchVisible by remember { mutableStateOf(false) }
    var draftSearchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState.appliedUserSearchQuery, uiState.appliedGroupSearchQuery) {
        if (uiState.currentSubScreen == AdminSubScreenEnum.USERS) draftSearchQuery = uiState.appliedUserSearchQuery
        if (uiState.currentSubScreen == AdminSubScreenEnum.GROUPS) draftSearchQuery = uiState.appliedGroupSearchQuery
    }

    if (uiState.currentSubScreen != AdminSubScreenEnum.MAIN) {
        BackHandler { 
            if (isSearchVisible) {
                isSearchVisible = false
            } else {
                viewModel.onEvent(AdminEvent.Navigate(AdminSubScreenEnum.MAIN))
            }
        }
    }

    BaseScreen(
        title = when(uiState.currentSubScreen) {
            AdminSubScreenEnum.USERS -> Loc.Admin.users
            AdminSubScreenEnum.GROUPS -> Loc.Settings.groups
            AdminSubScreenEnum.NOTIFICATIONS -> Loc.General.notifications
            else -> Loc.Admin.adminPanel
        },
        isLoading = uiState.isLoading,
        onBack = { 
            if (isSearchVisible) {
                isSearchVisible = false
            } else {
                if (uiState.currentSubScreen == AdminSubScreenEnum.MAIN) onBack() else viewModel.onEvent(AdminEvent.Navigate(AdminSubScreenEnum.MAIN))
            }
        },
        onRefresh = if (uiState.currentSubScreen == AdminSubScreenEnum.USERS || uiState.currentSubScreen == AdminSubScreenEnum.GROUPS) {
            { viewModel.onEvent(AdminEvent.Refresh) }
        } else null,
        isSearchVisible = isSearchVisible,
        searchQuery = draftSearchQuery,
        onSearchQueryChange = { draftSearchQuery = it },
        onSearchClose = {
            isSearchVisible = false
            if (uiState.currentSubScreen == AdminSubScreenEnum.USERS) {
                viewModel.onEvent(AdminEvent.UpdateUserSearchQuery(""))
                viewModel.onEvent(AdminEvent.ApplyUserSearch)
            } else if (uiState.currentSubScreen == AdminSubScreenEnum.GROUPS) {
                viewModel.onEvent(AdminEvent.UpdateGroupSearchQuery(""))
                viewModel.onEvent(AdminEvent.ApplyGroupSearch)
            }
        },
        onSearchTriggered = {
            if (uiState.currentSubScreen == AdminSubScreenEnum.USERS) {
                viewModel.onEvent(AdminEvent.UpdateUserSearchQuery(draftSearchQuery))
                viewModel.onEvent(AdminEvent.ApplyUserSearch)
            } else if (uiState.currentSubScreen == AdminSubScreenEnum.GROUPS) {
                viewModel.onEvent(AdminEvent.UpdateGroupSearchQuery(draftSearchQuery))
                viewModel.onEvent(AdminEvent.ApplyGroupSearch)
            }
        },
        searchPlaceholder = if (uiState.currentSubScreen == AdminSubScreenEnum.USERS) Loc.Admin.searchUsers else Loc.General.searchGroups,
        actions = {
            if (uiState.currentSubScreen == AdminSubScreenEnum.USERS || uiState.currentSubScreen == AdminSubScreenEnum.GROUPS) {
                TitleActionIcon(icon = Icons.Default.Search, onClick = { isSearchVisible = true })
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState.currentSubScreen) {
                AdminSubScreenEnum.MAIN -> AdminMainScreen(
                    onNavigate = { viewModel.onEvent(AdminEvent.Navigate(it)) }
                )
                AdminSubScreenEnum.USERS -> UsersTab(viewModel, uiState)
                AdminSubScreenEnum.GROUPS -> GroupsTab(viewModel, uiState)
                AdminSubScreenEnum.NOTIFICATIONS -> NotificationsTab(viewModel, uiState)
            }
        }
    }
}

@Composable
fun AdminMainScreen(
    onNavigate: (AdminSubScreenEnum) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AdminMenuItem(
                title = Loc.Admin.users,
                subtitle = Loc.Admin.usersSubtitle,
                icon = Icons.Default.People,
                onClick = { onNavigate(AdminSubScreenEnum.USERS) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = Loc.Settings.groups,
                subtitle = Loc.Admin.groupsSubtitle,
                icon = Icons.Default.GroupWork,
                onClick = { onNavigate(AdminSubScreenEnum.GROUPS) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = Loc.General.notifications,
                subtitle = Loc.Admin.sendNotifSubtitle,
                icon = Icons.Default.NotificationAdd,
                onClick = { onNavigate(AdminSubScreenEnum.NOTIFICATIONS) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = Loc.Admin.simulateCrash,
                subtitle = Loc.Admin.simulateCrashSubtitle,
                icon = Icons.Default.BugReport,
                onClick = { throw RuntimeException("Manual Crash Simulation") }
            )
        }
    }
}

@Composable
fun AdminMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ActionListItem(
        onClick = onClick,
        headline = title,
        supportingText = subtitle,
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Icon(
                imageVector = Icons.Default.ChevronRight, 
                contentDescription = null, 
                modifier = Modifier.scale(if (isRtl) -1f else 1f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    )
}

@Composable
fun UsersTab(viewModel: AdminViewModel, uiState: AdminUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            val filtered = if (uiState.appliedUserSearchQuery.isEmpty()) {
                emptyList()
            } else {
                uiState.users.filter { user ->
                    user.email?.contains(uiState.appliedUserSearchQuery, ignoreCase = true) == true ||
                    user.uid.contains(uiState.appliedUserSearchQuery, ignoreCase = true)
                }
            }

            items(filtered, key = { it.uid.ifEmpty { "empty_u_${System.identityHashCode(it)}" } }) { user ->
                val uid = user.uid
                val isAdmin = user.isAdmin
                val isReviewer = user.isReviewer
                var menuExpanded by remember { mutableStateOf(false) }

                ActionListItem(
                    headline = uiState.userNames[uid] ?: user.email ?: Loc.General.unknown,
                    supportingText = user.email ?: "",
                    metadataText = buildString {
                        if (isAdmin) append("ADMIN • ")
                        if (isReviewer) append("REVIEWER • ")
                        append(uid)
                    },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, null)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isAdmin) Loc.Admin.removeAdmin else Loc.Admin.makeAdmin) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.onEvent(AdminEvent.SetUserAdminStatus(uid, !isAdmin))
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            if (isAdmin) Icons.Default.PersonOff else Icons.Default.AdminPanelSettings, 
                                            null
                                        ) 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isReviewer) Loc.Admin.removeReviewer else Loc.Admin.makeReviewer) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.onEvent(AdminEvent.SetUserReviewerStatus(uid, !isReviewer))
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            if (isReviewer) Icons.Default.PersonOff else Icons.Default.RateReview, 
                                            null
                                        ) 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(Loc.General.delete, color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.onEvent(AdminEvent.RequestDeleteUser(uid, 1))
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
        }
    }

    if (uiState.showUserDeleteConfirm1 != null) {
        DeleteUserConfirmSheet(
            step = 1,
            onDismiss = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 1)) },
            onNext = { viewModel.onEvent(AdminEvent.RequestDeleteUser(uiState.showUserDeleteConfirm1, 2)) },
            onConfirm = {}
        )
    }

    if (uiState.showUserDeleteConfirm2 != null) {
        DeleteUserConfirmSheet(
            step = 2,
            onDismiss = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 2)) },
            onNext = {},
            onConfirm = { viewModel.onEvent(AdminEvent.ConfirmDeleteUser(uiState.showUserDeleteConfirm2!!)) }
        )
    }
}

@Composable
fun GroupsTab(viewModel: AdminViewModel, uiState: AdminUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            val filtered = if (uiState.appliedGroupSearchQuery.isEmpty()) {
                emptyList()
            } else {
                uiState.groups.filter { group ->
                    group.name.contains(uiState.appliedGroupSearchQuery, ignoreCase = true) ||
                    group.id.contains(uiState.appliedGroupSearchQuery, ignoreCase = true)
                }
            }

            items(filtered, key = { it.id.ifEmpty { "empty_g_${System.identityHashCode(it)}" } }) { group ->
                val gid = group.id
                ActionListItem(
                    headline = group.name,
                    supportingText = gid,
                    trailingContent = {
                        IconButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(gid, 1)) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
        }
    }

    if (uiState.showGroupDeleteConfirm1 != null) {
        DeleteGroupConfirmSheet(
            step = 1,
            onDismiss = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 1)) },
            onNext = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(uiState.showGroupDeleteConfirm1, 2)) },
            onConfirm = {}
        )
    }

    if (uiState.showGroupDeleteConfirm2 != null) {
        DeleteGroupConfirmSheet(
            step = 2,
            onDismiss = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 2)) },
            onNext = {},
            onConfirm = { viewModel.onEvent(AdminEvent.ConfirmDeleteGroup(uiState.showGroupDeleteConfirm2!!)) }
        )
    }
}

@Composable
fun NotificationsTab(viewModel: AdminViewModel, uiState: AdminUiState) {
    var showUserCreator by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showGroupCreator by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                SectionHeader("Search Groups")
                SearchField(
                    value = uiState.notifGroupSearchQuery,
                    onValueChange = { viewModel.onEvent(AdminEvent.UpdateNotifGroupSearch(it)) },
                    onSearch = { viewModel.onEvent(AdminEvent.ApplyNotifGroupSearch) },
                    placeholder = "Group Name or ID"
                )
            }
            
            if (uiState.appliedNotifGroupSearch.isNotEmpty()) {
                val filteredGroups = uiState.groups.filter { 
                    it.name.contains(uiState.appliedNotifGroupSearch, ignoreCase = true) || 
                    it.id.contains(uiState.appliedNotifGroupSearch, ignoreCase = true) 
                }
                items(filteredGroups) { group ->
                    ActionListItem(
                        headline = group.name,
                        supportingText = "Send notification to all members",
                        leadingContent = { Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showGroupCreator = group.id to group.name }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
            }
            
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader("Search Users")
                SearchField(
                    value = uiState.notifUserSearchQuery,
                    onValueChange = { viewModel.onEvent(AdminEvent.UpdateNotifUserSearch(it)) },
                    onSearch = { viewModel.onEvent(AdminEvent.ApplyNotifUserSearch) },
                    placeholder = "Email or UID"
                )
            }

            if (uiState.appliedNotifUserSearch.isNotEmpty()) {
                val filteredUsers = uiState.users.filter { user ->
                    user.email?.contains(uiState.appliedNotifUserSearch, ignoreCase = true) == true ||
                    user.uid.contains(uiState.appliedNotifUserSearch, ignoreCase = true)
                }
                items(filteredUsers) { user ->
                    val name = uiState.userNames[user.uid] ?: user.email ?: "Unknown User"
                    ActionListItem(
                        headline = name,
                        supportingText = user.email ?: user.uid,
                        leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { showUserCreator = user.uid to name }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
            }
        }
    }

    if (showUserCreator != null) {
        NotificationCreatorSheet(
            targetName = showUserCreator!!.second,
            onDismiss = { showUserCreator = null },
            onSend = { title, msg, type ->
                viewModel.onEvent(AdminEvent.SendPersonalNotification(showUserCreator!!.first, title, msg, type))
                showUserCreator = null
            }
        )
    }

    if (showGroupCreator != null) {
        NotificationCreatorSheet(
            targetName = showGroupCreator!!.second,
            onDismiss = { showGroupCreator = null },
            onSend = { title, msg, type ->
                viewModel.onEvent(AdminEvent.SendGroupNotification(showGroupCreator!!.first, title, msg, type))
                showGroupCreator = null
            }
        )
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, null)
            }
        }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
