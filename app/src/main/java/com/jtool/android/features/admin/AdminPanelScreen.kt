package com.jtool.android.features.admin

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleActionIcon
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    groupSyncManager: GroupSyncManager,
    onBack: () -> Unit,
    viewModel: AdminViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AdminViewModel(groupSyncManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var isSearchVisible by remember { mutableStateOf(false) }
    var draftSearchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState.appliedUserSearchQuery, uiState.appliedGroupSearchQuery) {
        if (uiState.currentSubScreen == "users") draftSearchQuery = uiState.appliedUserSearchQuery
        if (uiState.currentSubScreen == "groups") draftSearchQuery = uiState.appliedGroupSearchQuery
    }

    if (uiState.currentSubScreen != "main") {
        BackHandler { 
            if (isSearchVisible) {
                isSearchVisible = false
            } else {
                viewModel.onEvent(AdminEvent.Navigate(if (uiState.currentSubScreen == "request_detail") "requests" else "main"))
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            GenericTitle(
                title = when(uiState.currentSubScreen) {
                    "users" -> Loc.admin_users
                    "groups" -> Loc.settings_groups
                    "migration" -> "System Data Migration"
                    else -> Loc.admin_adminPanel
                },
                onBack = { 
                    if (isSearchVisible) {
                        isSearchVisible = false
                    } else {
                        if (uiState.currentSubScreen == "main") onBack() else viewModel.onEvent(AdminEvent.Navigate("main"))
                    }
                },
                isLoading = uiState.isMigrating,
                isSearchVisible = isSearchVisible,
                searchQuery = draftSearchQuery,
                onSearchQueryChange = { draftSearchQuery = it },
                onSearchClose = {
                    isSearchVisible = false
                    if (uiState.currentSubScreen == "users") {
                        viewModel.onEvent(AdminEvent.UpdateUserSearchQuery(""))
                        viewModel.onEvent(AdminEvent.ApplyUserSearch)
                    } else if (uiState.currentSubScreen == "groups") {
                        viewModel.onEvent(AdminEvent.UpdateGroupSearchQuery(""))
                        viewModel.onEvent(AdminEvent.ApplyGroupSearch)
                    }
                },
                onSearchTriggered = {
                    if (uiState.currentSubScreen == "users") {
                        viewModel.onEvent(AdminEvent.UpdateUserSearchQuery(draftSearchQuery))
                        viewModel.onEvent(AdminEvent.ApplyUserSearch)
                    } else if (uiState.currentSubScreen == "groups") {
                        viewModel.onEvent(AdminEvent.UpdateGroupSearchQuery(draftSearchQuery))
                        viewModel.onEvent(AdminEvent.ApplyGroupSearch)
                    }
                },
                onRefresh = if (uiState.currentSubScreen == "users" || uiState.currentSubScreen == "groups") {
                    { viewModel.onEvent(AdminEvent.Refresh) }
                } else null,
                searchPlaceholder = if (uiState.currentSubScreen == "users") Loc.admin_searchUsers else Loc.general_searchGroups,
                actions = {
                    if (uiState.currentSubScreen == "users" || uiState.currentSubScreen == "groups") {
                        TitleActionIcon(icon = Icons.Default.Search, onClick = { isSearchVisible = true })
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState.currentSubScreen) {
                "main" -> AdminMainScreen(onNavigate = { viewModel.onEvent(AdminEvent.Navigate(it)) })
                "users" -> UsersTab(viewModel, uiState)
                "groups" -> GroupsTab(viewModel, uiState)
                "migration" -> MigrationTab(viewModel, uiState)
            }
        }
    }
}

@Composable
fun MigrationTab(viewModel: AdminViewModel, uiState: AdminUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("System Data Migration", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        if (uiState.isMigrating) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
        }
        
        uiState.migrationStatus?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("1. Legacy Fields Migration", style = MaterialTheme.typography.titleMedium)
        Text("Updates legacy fields (edited, pending, checked, enabled, recurring) to new standardized names (isEdited, isPending, isChecked, isEnabled, isRecurring).", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.onEvent(AdminEvent.StartLegacyFieldsMigration) },
            enabled = !uiState.isMigrating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Legacy Fields Migration")
        }
    }
}

@Composable
fun AdminMainScreen(onNavigate: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AdminMenuItem(
                title = Loc.admin_users,
                subtitle = Loc.admin_usersSubtitle,
                icon = Icons.Default.People,
                onClick = { onNavigate("users") }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = Loc.settings_groups,
                subtitle = Loc.admin_groupsSubtitle,
                icon = Icons.Default.GroupWork,
                onClick = { onNavigate("groups") }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = "System Migration",
                subtitle = "Standardize database field names",
                icon = Icons.Default.Backup,
                onClick = { onNavigate("migration") }
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
                uiState.users.filter { 
                    (it[com.jtool.android.core.models.User::email.name] as? String)?.contains(uiState.appliedUserSearchQuery, ignoreCase = true) == true ||
                    (it[com.jtool.android.core.models.UserData::appUserId.name] as? String)?.contains(uiState.appliedUserSearchQuery, ignoreCase = true) == true ||
                    (it[com.jtool.android.core.models.User::uid.name] as? String)?.contains(uiState.appliedUserSearchQuery, ignoreCase = true) == true
                }
            }

            items(filtered) { user ->
                val uid = user[com.jtool.android.core.models.User::uid.name] as? String ?: ""
                val isAdmin = user[com.jtool.android.core.models.User::isAdmin.name] as? Boolean ?: false
                var menuExpanded by remember { mutableStateOf(false) }

                ActionListItem(
                    headline = uiState.userNames[uid] ?: (user["userName"] as? String) ?: Loc.general_unknown,
                    supportingText = user[com.jtool.android.core.models.User::email.name] as? String ?: "",
                    metadataText = buildString {
                        if (isAdmin) append("ADMIN • ")
                        append(user[com.jtool.android.core.models.User::uid.name] as? String ?: "")
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
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 1)) },
            title = { Text(Loc.general_delete) },
            text = { Text(Loc.admin_deleteUserPermanently) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.RequestDeleteUser(uiState.showUserDeleteConfirm1, 2))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.general_delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 1)) }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }

    if (uiState.showUserDeleteConfirm2 != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 2)) },
            title = { Text(Loc.general_areYouSure) },
            text = { Text(Loc.general_confirmDeleteMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.ConfirmDeleteUser(uiState.showUserDeleteConfirm2))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.general_delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 2)) }) {
                    Text(Loc.general_cancel)
                }
            }
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
                uiState.groups.filter {
                    (it[com.jtool.android.core.models.GroupInfo::name.name] as? String)?.contains(uiState.appliedGroupSearchQuery, ignoreCase = true) == true ||
                    (it[com.jtool.android.core.models.GroupInfo::id.name] as? String)?.contains(uiState.appliedGroupSearchQuery, ignoreCase = true) == true
                }
            }

            items(filtered) { group ->
                val gid = group[com.jtool.android.core.models.GroupInfo::id.name] as? String ?: ""
                ActionListItem(
                    headline = group[com.jtool.android.core.models.GroupInfo::name.name] as? String ?: "Unknown",
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
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 1)) },
            title = { Text(Loc.admin_deleteGroup) },
            text = { Text(Loc.admin_deleteGroupPermanently) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.RequestDeleteGroup(uiState.showGroupDeleteConfirm1, 2))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.general_delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 1)) }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }

    if (uiState.showGroupDeleteConfirm2 != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 2)) },
            title = { Text(Loc.general_areYouSure) },
            text = { Text(Loc.general_confirmDeleteMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.ConfirmDeleteGroup(uiState.showGroupDeleteConfirm2))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.general_delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 2)) }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }
}
