package com.example.jtool

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jtool.settingsSections.*

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
    onGroupChanged: (String?) -> Unit // Added this
) {
    var currentSubScreen by remember { 
        mutableStateOf(if (startOnUpdates) "app_updates" else "main") 
    }
    var linkedUriString by remember { mutableStateOf(settingsManager.getSyncUri()) }
    var availableUpdateVersion by remember { mutableStateOf<Int?>(null) }
    val currentUser = authManager.getCurrentUser()

    LaunchedEffect(linkedUriString) {
        availableUpdateVersion = backupManager.checkForUpdate(linkedUriString)
    }

    if (currentSubScreen != "main") {
        BackHandler { 
            currentSubScreen = "main" 
        }
    }

    // Reset the redirect flag when leaving Settings to ensure normal entry next time
    LaunchedEffect(currentSubScreen) {
        // No easy way to reset the flag in MainActivity from here without a callback, 
        // but the current implementation in MainActivity resets on recompose or screen change
    }

    when (currentSubScreen) {
        "main" -> SettingsMainScreen(
            onNavigate = { currentSubScreen = it },
            hasUpdate = hasUpdate,
            isSyncMissing = linkedUriString.isNullOrEmpty() && currentUser == null,
            settingsManager = settingsManager,
            isDarkMode = isDarkMode,
            onThemeToggle = onThemeToggle,
            currentLang = currentLang,
            onLangToggle = onLangToggle,
            currentUser = currentUser,
            onSignIn = onSignIn,
            onSignOut = { authManager.signOut() }
        )
        "sync" -> GroupsScreen(
            groupSyncManager = groupSyncManager,
            settingsManager = settingsManager,
            onBack = { currentSubScreen = "main" },
            onGroupChanged = onGroupChanged // Pass it down
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
            backupManager = backupManager,
            settingsManager = settingsManager,
            linkedUri = linkedUriString,
            onBack = { currentSubScreen = "main" }
        )
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
