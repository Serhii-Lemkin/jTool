package com.example.jtool

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    groupSyncManager: GroupSyncManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf("main") }
    var selectedRequest by remember { mutableStateOf<Map<String, String>?>(null) }

    if (currentSubScreen != "main") {
        BackHandler { 
            if (currentSubScreen == "request_detail") currentSubScreen = "requests"
            else currentSubScreen = "main" 
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(currentSubScreen) {
                            "app_info" -> Loc.appInfo
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
                        if (currentSubScreen == "main") onBack() 
                        else if (currentSubScreen == "request_detail") currentSubScreen = "requests"
                        else currentSubScreen = "main"
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Loc.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentSubScreen) {
                "main" -> AdminMainScreen(onNavigate = { currentSubScreen = it })
                "app_info" -> AppInfoTab(groupSyncManager)
                "requests" -> {
                    val userNames by groupSyncManager.userNamesFlow().collectAsState(initial = emptyMap())
                    RequestsTab(
                        manager = groupSyncManager,
                        userNames = userNames,
                        onSelectRequest = { 
                            selectedRequest = it
                            currentSubScreen = "request_detail"
                        }
                    )
                }
                "request_detail" -> selectedRequest?.let { req ->
                    val userNames by groupSyncManager.userNamesFlow().collectAsState(initial = emptyMap())
                    RequestDetailScreen(
                        request = req,
                        userName = userNames[req["userId"]] ?: (req["userId"] ?: "Unknown"),
                        manager = groupSyncManager,
                        onBack = { currentSubScreen = "requests" }
                    )
                }
                "users" -> UsersTab(groupSyncManager)
                "groups" -> GroupsTab(groupSyncManager)
            }
        }
    }
}

@Composable
fun AdminMainScreen(onNavigate: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AdminMenuItem(
                title = Loc.appInfo,
                subtitle = Loc.appInfoSubtitle,
                icon = Icons.Default.Info,
                onClick = { onNavigate("app_info") }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
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
fun AppInfoTab(manager: GroupSyncManager) {
    var versionCode by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        val info = manager.getLatestAppInfo()
        versionCode = info?.get("versionCode")?.toString() ?: ""
        downloadUrl = info?.get("downloadUrl")?.toString() ?: ""
    }
    
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        OutlinedTextField(
            value = versionCode,
            onValueChange = { versionCode = it },
            label = { Text(Loc.versionCode) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = downloadUrl,
            onValueChange = { downloadUrl = it },
            label = { Text(Loc.downloadUrl) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    manager.updateAppInfo(versionCode.toIntOrNull() ?: 0, downloadUrl)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Loc.updateAppInfo)
        }
    }
}

@Composable
fun RequestsTab(
    manager: GroupSyncManager, 
    userNames: Map<String, String>,
    onSelectRequest: (Map<String, String>) -> Unit
) {
    val requests by manager.allSuggestionsFlow().collectAsState(initial = emptyList())
    
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
    manager: GroupSyncManager, 
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = request["text"] ?: "", fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Text(text = "${Loc.by}: $userName", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    manager.deleteSuggestion(request["id"]!!)
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(Loc.closeRequest)
        }
    }
}

@Composable
fun UsersTab(manager: GroupSyncManager) {
    val users by manager.allUsersFlow().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(Loc.searchUsers) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            val filtered = users.filter { 
                (it["userName"] as? String)?.contains(searchQuery, ignoreCase = true) == true ||
                (it["appUserId"] as? String)?.contains(searchQuery, ignoreCase = true) == true ||
                (it["uid"] as? String)?.contains(searchQuery, ignoreCase = true) == true
            }.take(10)

            items(filtered) { user ->
                val isAdmin = user["isAdmin"] as? Boolean ?: false
                ListItem(
                    headlineContent = { Text(user["userName"] as? String ?: Loc.t("Unknown", "Невідомо")) },
                    supportingContent = { Text(user["uid"] as? String ?: "") },
                    trailingContent = {
                        Switch(
                            checked = isAdmin,
                            onCheckedChange = { 
                                scope.launch {
                                    manager.setUserAdminStatus(user["uid"] as String, it)
                                }
                            }
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun GroupsTab(manager: GroupSyncManager) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text(Loc.groupMaintenance, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    manager.deleteEmptyGroups()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(Loc.deleteEmptyGroups)
        }
    }
}
