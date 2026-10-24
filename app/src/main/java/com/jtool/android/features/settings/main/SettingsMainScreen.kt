package com.jtool.android.features.settings.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.config.Config
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.settings.SettingsMenuItem
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
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
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    if (infoMessage != null) {
        MessageSheet(
            message = infoMessage!!,
            type = MessageType.ERROR,
            onDismiss = { infoMessage = null }
        )
    }

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
                    title = Loc.General.aboutApp,
                    subtitle = Loc.General.landingPageSubtitle,
                    icon = Icons.Default.Info,
                    onClick = {
                        val dataManager = DataManager(context)
                        com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "")
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            item {
                SettingsMenuItem(
                    title = Loc.Settings.privacyPolicy,
                    subtitle = Loc.Settings.privacySubtitle,
                    icon = Icons.Default.PrivacyTip,
                    onClick = {
                        val dataManager = DataManager(context)
                        com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "privacy")
                    }
                )
            }
        }
    }
}
