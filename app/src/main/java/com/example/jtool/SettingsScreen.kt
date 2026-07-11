package com.example.jtool

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jtool.settingsSections.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    groupSyncManager: GroupSyncManager,
    onSignIn: () -> Unit,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    currentLang: String,
    onLangToggle: () -> Unit,
    hasUpdate: Boolean = false,
    startOnUpdates: Boolean = false,
    startOnGroups: Boolean = false,
    startOnAccount: Boolean = false,
    onGroupChanged: (String?) -> Unit,
    groups: Map<String, JGroupInfo>,
    onGroupsUpdated: (Map<String, JGroupInfo>) -> Unit,
    currentGroupId: String?,
    onBack: () -> Unit
) {
    var currentSubScreen by remember { 
        mutableStateOf(
            if (startOnUpdates) "app_updates" 
            else if (startOnGroups) "groups"
            else if (startOnAccount) "account"
            else "main"
        )
    }
    val currentUser = authManager.getCurrentUser()

    if (currentSubScreen != "main") {
        BackHandler { currentSubScreen = "main" }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(currentSubScreen) {
                            "account" -> Loc.account
                            "groups" -> Loc.groups
                            "general" -> Loc.generalSettings
                            "app_updates" -> Loc.appUpdates
                            "visibility" -> Loc.visibilityTitle
                            "requests" -> Loc.requestsTitle
                            else -> Loc.settings
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentSubScreen == "main") onBack() else currentSubScreen = "main" 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Loc.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentSubScreen) {
                "main" -> SettingsMainScreen(
                    onNavigate = { currentSubScreen = it },
                    hasUpdate = hasUpdate,
                    isSyncMissing = currentUser == null || currentGroupId == null,
                    currentUser = currentUser
                )
                "account" -> AccountScreen(
                    settingsManager = settingsManager,
                    currentUser = currentUser,
                    onSignIn = onSignIn,
                    onSignOut = { 
                        authManager.signOut()
                        onGroupChanged(null)
                        onGroupsUpdated(emptyMap())
                        currentSubScreen = "main"
                    }
                )
                "groups" -> GroupsScreen(
                    groupSyncManager = groupSyncManager,
                    settingsManager = settingsManager,
                    onBack = { currentSubScreen = "main" },
                    onGroupChanged = onGroupChanged,
                    onGroupsUpdated = onGroupsUpdated,
                    currentGroupId = currentGroupId,
                    groups = groups
                )
                "general" -> GeneralSettingsScreen(
                    settingsManager = settingsManager,
                    isDarkMode = isDarkMode,
                    onThemeToggle = onThemeToggle,
                    currentLang = currentLang,
                    onLangToggle = onLangToggle
                )
                "app_updates" -> AppUpdatesScreen(
                    backupManager = backupManager,
                    groupSyncManager = groupSyncManager,
                    settingsManager = settingsManager,
                    onBack = { currentSubScreen = "main" }
                )
                "visibility" -> AppVisibilityScreen(
                    settingsManager = settingsManager,
                    onBack = { currentSubScreen = "main" }
                )
                "requests" -> RequestsScreen(
                    groupSyncManager = groupSyncManager,
                    settingsManager = settingsManager,
                    onBack = { currentSubScreen = "main" }
                )
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String, 
    subtitle: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    onClick: () -> Unit,
    hasBadge: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (hasBadge) {
                    Badge(containerColor = MaterialTheme.colorScheme.error)
                }
            }
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
