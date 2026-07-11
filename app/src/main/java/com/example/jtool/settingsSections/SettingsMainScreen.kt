package com.example.jtool.settingsSections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jtool.Loc
import com.example.jtool.SettingsMenuItem

@Composable
fun SettingsMainScreen(
    onNavigate: (String) -> Unit,
    hasUpdate: Boolean,
    isSyncMissing: Boolean,
    currentUser: com.google.firebase.auth.FirebaseUser?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsMenuItem(
                title = Loc.account,
                subtitle = Loc.cloudSyncFirebase,
                icon = Icons.Default.AccountCircle,
                onClick = { onNavigate("account") }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        }
        
        if (currentUser != null) {
            item {
                SettingsMenuItem(
                    title = Loc.groups,
                    subtitle = Loc.syncSubtitle,
                    icon = Icons.Default.Group,
                    onClick = { onNavigate("groups") },
                    hasBadge = isSyncMissing
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.generalSettings,
                    subtitle = Loc.appearanceLanguage,
                    icon = Icons.Default.Settings,
                    onClick = { onNavigate("general") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.appUpdates,
                    subtitle = Loc.appUpdatesSubtitle,
                    icon = Icons.Default.SystemUpdate,
                    onClick = { onNavigate("app_updates") },
                    hasBadge = hasUpdate
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.visibilityTitle,
                    subtitle = Loc.visibilitySubtitle,
                    icon = Icons.Default.Visibility,
                    onClick = { onNavigate("visibility") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.requestsTitle,
                    subtitle = Loc.requestsSubtitle,
                    icon = Icons.Default.Feedback,
                    onClick = { onNavigate("requests") }
                )
            }
        }
    }
}
