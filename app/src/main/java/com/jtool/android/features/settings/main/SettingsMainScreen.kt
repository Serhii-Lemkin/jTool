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
                title = Loc.Settings.account,
                subtitle = Loc.Settings.cloudSyncFirebase,
                icon = Icons.Default.AccountCircle,
                onClick = { onNavigate(AppScreenEnum.SETTINGS_ACCOUNT) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        }
        
        if (uiState.currentUser != null) {
            item {
                SettingsMenuItem(
                    title = Loc.Settings.groups,
                    subtitle = Loc.Settings.syncSubtitle,
                    icon = Icons.Default.Group,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_GROUPS) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.Settings.generalSettings,
                    subtitle = Loc.Settings.appearanceLanguage,
                    icon = Icons.Default.Settings,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_GENERAL) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.Settings.appUpdates,
                    subtitle = Loc.Settings.appUpdatesSubtitle,
                    icon = Icons.Default.SystemUpdate,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_UPDATES) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.Settings.interfaceAndApps,
                    subtitle = Loc.Settings.visibilitySubtitle,
                    icon = Icons.Default.Visibility,
                    onClick = { onNavigate(AppScreenEnum.SETTINGS_VISIBILITY) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = rememberCoroutineScope()
                SettingsMenuItem(
                    title = Loc.Settings.privacyPolicy,
                    subtitle = Loc.Settings.privacySubtitle,
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
