package com.jtool.android.features.admin

import com.jtool.android.core.models.admin.enums.AdminSubScreenEnum
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.features.admin.components.cleanup_confirm_slip.CleanupConfirmSlip
import com.jtool.android.features.admin.components.delete_user_confirm_slip.DeleteUserConfirmSlip
import com.jtool.android.features.settings.groups.components.delete_group_confirm_slip.DeleteGroupConfirmSlip
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
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

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            GenericTitle(
                title = when(uiState.currentSubScreen) {
                    AdminSubScreenEnum.USERS -> Loc.admin_users
                    AdminSubScreenEnum.GROUPS -> Loc.settings_groups
                    else -> Loc.admin_adminPanel
                },
                onBack = { 
                    if (isSearchVisible) {
                        isSearchVisible = false
                    } else {
                        if (uiState.currentSubScreen == AdminSubScreenEnum.MAIN) onBack() else viewModel.onEvent(AdminEvent.Navigate(AdminSubScreenEnum.MAIN))
                    }
                },
                isLoading = uiState.isLoading,
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
                onRefresh = if (uiState.currentSubScreen == AdminSubScreenEnum.USERS || uiState.currentSubScreen == AdminSubScreenEnum.GROUPS) {
                    { viewModel.onEvent(AdminEvent.Refresh) }
                } else null,
                searchPlaceholder = if (uiState.currentSubScreen == AdminSubScreenEnum.USERS) Loc.admin_searchUsers else Loc.general_searchGroups,
                actions = {
                    if (uiState.currentSubScreen == AdminSubScreenEnum.USERS || uiState.currentSubScreen == AdminSubScreenEnum.GROUPS) {
                        TitleActionIcon(icon = Icons.Default.Search, onClick = { isSearchVisible = true })
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState.currentSubScreen) {
                AdminSubScreenEnum.MAIN -> AdminMainScreen(
                    onNavigate = { viewModel.onEvent(AdminEvent.Navigate(it)) },
                    onEvent = { viewModel.onEvent(it) }
                )
                AdminSubScreenEnum.USERS -> UsersTab(viewModel, uiState)
                AdminSubScreenEnum.GROUPS -> GroupsTab(viewModel, uiState)
            }
        }
    }
}



@Composable
fun AdminMainScreen(onNavigate: (AdminSubScreenEnum) -> Unit, onEvent: (AdminEvent) -> Unit) {
    var showCleanupConfirm by remember { mutableStateOf(false) }

    if (showCleanupConfirm) {
        CleanupConfirmSlip(
            onDismiss = { showCleanupConfirm = false },
            onConfirm = {
                onEvent(AdminEvent.CleanupLegacyWorkHours)
                showCleanupConfirm = false
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AdminMenuItem(
                title = Loc.admin_users,
                subtitle = Loc.admin_usersSubtitle,
                icon = Icons.Default.People,
                onClick = { onNavigate(AdminSubScreenEnum.USERS) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = Loc.settings_groups,
                subtitle = Loc.admin_groupsSubtitle,
                icon = Icons.Default.GroupWork,
                onClick = { onNavigate(AdminSubScreenEnum.GROUPS) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = Loc.admin_cleanupLegacyWorkHours,
                subtitle = "Delete old 'hours' field from all UserData",
                icon = Icons.Default.DeleteSweep,
                onClick = { showCleanupConfirm = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = "Simulate Crash",
                subtitle = "Triggers a fatal exception to test crash reporting",
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
                var menuExpanded by remember { mutableStateOf(false) }

                ActionListItem(
                    headline = uiState.userNames[uid] ?: user.email ?: Loc.general_unknown,
                    supportingText = user.email ?: "",
                    metadataText = buildString {
                        if (isAdmin) append("ADMIN • ")
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
                                    text = { Text(if (isAdmin) Loc.admin_removeAdmin else Loc.admin_makeAdmin) },
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
                                    text = { Text(Loc.general_delete, color = MaterialTheme.colorScheme.error) },
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
        DeleteUserConfirmSlip(
            step = 1,
            onDismiss = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 1)) },
            onNext = { viewModel.onEvent(AdminEvent.RequestDeleteUser(uiState.showUserDeleteConfirm1, 2)) },
            onConfirm = {}
        )
    }

    if (uiState.showUserDeleteConfirm2 != null) {
        DeleteUserConfirmSlip(
            step = 2,
            onDismiss = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 2)) },
            onNext = {},
            onConfirm = { viewModel.onEvent(AdminEvent.ConfirmDeleteUser(uiState.showUserDeleteConfirm2)) }
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
        DeleteGroupConfirmSlip(
            step = 1,
            onDismiss = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 1)) },
            onNext = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(uiState.showGroupDeleteConfirm1, 2)) },
            onConfirm = {}
        )
    }

    if (uiState.showGroupDeleteConfirm2 != null) {
        DeleteGroupConfirmSlip(
            step = 2,
            onDismiss = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 2)) },
            onNext = {},
            onConfirm = { viewModel.onEvent(AdminEvent.ConfirmDeleteGroup(uiState.showGroupDeleteConfirm2)) }
        )
    }
}
