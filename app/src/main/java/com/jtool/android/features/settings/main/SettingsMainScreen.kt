package com.jtool.android.features.settings.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.settings.SettingsMenuItem
import com.jtool.android.core.models.common.enums.AppScreenEnum
import kotlinx.coroutines.launch

@Composable
fun SettingsMainScreen(
    authManager: AuthManager,
    settingsManager: SettingsManager,
    onNavigate: (AppScreenEnum) -> Unit,
    hasUpdate: Boolean,
    viewModel: SettingsMainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsMainViewModel(authManager, settingsManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsMenuItem(
                title = Loc.settings_account,
                subtitle = Loc.settings_cloudSyncFirebase,
                icon = Icons.Default.AccountCircle,
                onClick = { onNavigate(AppScreenEnum.SETTINGS_ACCOUNT) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        }
        
        if (uiState.currentUser != null) {
            item {
                SettingsMenuItem(
                    title = Loc.settings_groups,
                    subtitle = Loc.settings_syncSubtitle,
                    icon = Icons.Default.Group,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_GROUPS) },
                    hasBadge = uiState.isSyncMissing
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.settings_generalSettings,
                    subtitle = Loc.settings_appearanceLanguage,
                    icon = Icons.Default.Settings,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_GENERAL) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.settings_appUpdates,
                    subtitle = Loc.settings_appUpdatesSubtitle,
                    icon = Icons.Default.SystemUpdate,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_UPDATES) },
                    hasBadge = hasUpdate
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.settings_visibilityTitle,
                    subtitle = Loc.settings_visibilitySubtitle,
                    icon = Icons.Default.Visibility,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_VISIBILITY) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.support_hub_title,
                    subtitle = Loc.support_new_request,
                    icon = Icons.Default.Feedback,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_REQUESTS) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = rememberCoroutineScope()
                SettingsMenuItem(
                    title = Loc.settings_privacyPolicy,
                    subtitle = Loc.settings_privacySubtitle,
                    icon = Icons.Default.PrivacyTip,
                    onClick = {
                        val dataManager = DataManager(context)
                        scope.launch {
                            val options = dataManager.getAppOptions()
                            val url = options?.privacy
                            if (!url.isNullOrBlank()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } else {
                                android.util.Log.e("jTool", "Privacy Policy URL is missing in Firebase")
                                android.widget.Toast.makeText(context, "Privacy Policy URL not found", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
