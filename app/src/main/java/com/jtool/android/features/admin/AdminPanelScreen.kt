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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.sync.GroupSyncManager
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

    if (uiState.currentSubScreen != "main") {
        BackHandler { 
            viewModel.onEvent(AdminEvent.Navigate(if (uiState.currentSubScreen == "request_detail") "requests" else "main"))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(uiState.currentSubScreen) {
                            "requests" -> Loc.requestsTitle
                            "request_detail" -> Loc.requestDetails
                            "users" -> Loc.users
                            "groups" -> Loc.groups
                            else -> Loc.adminPanel
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        when (uiState.currentSubScreen) {
                            "main" -> onBack()
                            "request_detail" -> viewModel.onEvent(AdminEvent.Navigate("requests"))
                            else -> viewModel.onEvent(AdminEvent.Navigate("main"))
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Loc.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState.currentSubScreen) {
                "main" -> AdminMainScreen(onNavigate = { viewModel.onEvent(AdminEvent.Navigate(it)) })
                "requests" -> {
                    RequestsTab(
                        requests = uiState.requests,
                        userNames = uiState.userNames,
                        onSelectRequest = { 
                            viewModel.onEvent(AdminEvent.SelectRequest(it))
                        }
                    )
                }
                "request_detail" -> uiState.selectedRequest?.let { req ->
                    RequestDetailScreen(
                        request = req,
                        userName = uiState.userNames[req["userId"]] ?: (req["userId"] ?: "Unknown"),
                        onDelete = { viewModel.onEvent(AdminEvent.DeleteRequest(it)) },
                        onBack = { viewModel.onEvent(AdminEvent.Navigate("requests")) }
                    )
                }
                "users" -> UsersTab(viewModel, uiState)
                "groups" -> GroupsTab(viewModel, uiState)
            }
        }
    }
}

@Composable
fun AdminMainScreen(onNavigate: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AdminMenuItem(
                title = Loc.requestsTitle,
                subtitle = Loc.requestsSubtitleAdmin,
                icon = Icons.Default.Feedback,
                onClick = { onNavigate("requests") }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = Loc.users,
                subtitle = Loc.usersSubtitle,
                icon = Icons.Default.People,
                onClick = { onNavigate("users") }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            AdminMenuItem(
                title = Loc.groups,
                subtitle = Loc.groupsSubtitle,
                icon = Icons.Default.GroupWork,
                onClick = { onNavigate("groups") }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun RequestsTab(
    requests: List<Map<String, String>>, 
    userNames: Map<String, String>,
    onSelectRequest: (Map<String, String>) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(requests) { req ->
            val uId = req["userId"] ?: ""
            ListItem(
                headlineContent = { 
                    Text(
                        text = req["text"] ?: "", 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis 
                    ) 
                },
                supportingContent = { Text("${Loc.by}: ${userNames[uId] ?: uId}") },
                modifier = Modifier.clickable { onSelectRequest(req) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun RequestDetailScreen(
    request: Map<String, String>, 
    userName: String,
    onDelete: (String) -> Unit, 
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = request["text"] ?: "", fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Text(text = "${Loc.by}: $userName", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                onDelete(request["id"]!!)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(Loc.closeRequest)
        }
    }
}

@Composable
fun UsersTab(viewModel: AdminViewModel, uiState: AdminUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.userSearchQuery,
                onValueChange = { viewModel.onEvent(AdminEvent.UpdateUserSearchQuery(it)) },
                label = { Text(Loc.searchUsers) },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { viewModel.onEvent(AdminEvent.ApplyUserSearch) }) {
                Text(Loc.search)
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            val filtered = if (uiState.appliedUserSearchQuery.isEmpty()) {
                emptyList()
            } else {
                uiState.users.filter { 
                    (it["email"] as? String)?.contains(uiState.appliedUserSearchQuery, ignoreCase = true) == true ||
                    (it["appUserId"] as? String)?.contains(uiState.appliedUserSearchQuery, ignoreCase = true) == true ||
                    (it["uid"] as? String)?.contains(uiState.appliedUserSearchQuery, ignoreCase = true) == true
                }
            }

            items(filtered) { user ->
                val uid = user["uid"] as? String ?: ""
                val isAdmin = user["isAdmin"] as? Boolean ?: false
                var menuExpanded by remember { mutableStateOf(false) }

                ListItem(
                    headlineContent = { 
                        Text(uiState.userNames[uid] ?: (user["userName"] as? String) ?: Loc.t("Unknown", "Невідомо")) 
                    },
                    supportingContent = { 
                        Column {
                            Text(user["email"] as? String ?: "")
                            Text(user["uid"] as? String ?: "", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isAdmin,
                                onCheckedChange = { 
                                    viewModel.onEvent(AdminEvent.SetUserAdminStatus(uid, it))
                                }
                            )
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(Loc.delete, color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.onEvent(AdminEvent.RequestDeleteUser(uid, 1))
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (uiState.showUserDeleteConfirm1 != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 1)) },
            title = { Text(Loc.delete) },
            text = { Text(Loc.deleteUserPermanently) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.RequestDeleteUser(uiState.showUserDeleteConfirm1, 2))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 1)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (uiState.showUserDeleteConfirm2 != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 2)) },
            title = { Text(Loc.areYouSure) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.ConfirmDeleteUser(uiState.showUserDeleteConfirm2))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteUser(null, 2)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }
}

@Composable
fun GroupsTab(viewModel: AdminViewModel, uiState: AdminUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.groupSearchQuery,
                onValueChange = { viewModel.onEvent(AdminEvent.UpdateGroupSearchQuery(it)) },
                label = { Text(Loc.searchGroups) },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { viewModel.onEvent(AdminEvent.ApplyGroupSearch) }) {
                Text(Loc.search)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            val filtered = if (uiState.appliedGroupSearchQuery.isEmpty()) {
                emptyList()
            } else {
                uiState.groups.filter {
                    (it["name"] as? String)?.contains(uiState.appliedGroupSearchQuery, ignoreCase = true) == true ||
                    (it["id"] as? String)?.contains(uiState.appliedGroupSearchQuery, ignoreCase = true) == true
                }
            }

            items(filtered) { group ->
                val gid = group["id"] as? String ?: ""
                ListItem(
                    headlineContent = { Text(group["name"] as? String ?: "Unknown") },
                    supportingContent = { Text(gid) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(gid, 1)) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (uiState.showGroupDeleteConfirm1 != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 1)) },
            title = { Text(Loc.deleteGroup) },
            text = { Text(Loc.deleteGroupPermanently) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.RequestDeleteGroup(uiState.showGroupDeleteConfirm1, 2))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 1)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (uiState.showGroupDeleteConfirm2 != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 2)) },
            title = { Text(Loc.areYouSure) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(AdminEvent.ConfirmDeleteGroup(uiState.showGroupDeleteConfirm2))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Loc.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.RequestDeleteGroup(null, 2)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }
}
