package com.jtool.android.features.settings

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager

import com.jtool.android.features.settings.main.SettingsMainScreen
import com.jtool.android.features.settings.account.AccountScreen
import com.jtool.android.features.settings.groups.GroupsScreen
import com.jtool.android.features.settings.general.GeneralSettingsScreen
import com.jtool.android.features.settings.updates.AppUpdatesScreen
import com.jtool.android.features.settings.visibility.AppVisibilityScreen
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.models.common.enums.AppScreenEnum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    dataManager: DataManager,
    onSignIn: () -> Unit,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    currentLang: String,
    onLangToggle: () -> Unit,
    startOnUpdates: Boolean = false,
    startOnGroups: Boolean = false,
    startOnAccount: Boolean = false,
    onGroupChanged: (String?) -> Unit,
    groups: Map<String, GroupInfo>,
    onGroupsUpdated: (Map<String, GroupInfo>) -> Unit,
    currentGroupId: String?,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(startOnUpdates, startOnGroups, startOnAccount) {
        when {
            startOnUpdates -> viewModel.onEvent(SettingsEvent.Navigate(AppScreenEnum.SETTINGS_UPDATES))
            startOnGroups -> viewModel.onEvent(SettingsEvent.Navigate(AppScreenEnum.SETTINGS_GROUPS))
            startOnAccount -> viewModel.onEvent(SettingsEvent.Navigate(AppScreenEnum.SETTINGS_ACCOUNT))
            else -> viewModel.onEvent(SettingsEvent.Navigate(AppScreenEnum.SETTINGS))
        }
    }

    if (uiState.currentSubScreen != AppScreenEnum.SETTINGS) {
        BackHandler { viewModel.onEvent(SettingsEvent.Navigate(AppScreenEnum.SETTINGS)) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            if (uiState.currentSubScreen != AppScreenEnum.SETTINGS_GROUPS) {
                GenericTitle(
                    title = when(uiState.currentSubScreen) {
                        AppScreenEnum.SETTINGS_ACCOUNT -> Loc.Settings.account
                        AppScreenEnum.SETTINGS_GENERAL -> Loc.Settings.generalSettings
                        AppScreenEnum.SETTINGS_UPDATES -> Loc.Settings.appUpdates
                        AppScreenEnum.SETTINGS_VISIBILITY -> Loc.Settings.interfaceAndApps
                        else -> Loc.Launcher.settings
                    },
                    onBack = { 
                        if (uiState.currentSubScreen == AppScreenEnum.SETTINGS) onBack() else viewModel.onEvent(SettingsEvent.Navigate(AppScreenEnum.SETTINGS)) 
                    }
                )
            }
        }
    ) { padding ->
        val boxModifier = if (uiState.currentSubScreen == AppScreenEnum.SETTINGS_GROUPS) {
            Modifier.fillMaxSize()
        } else {
            Modifier.padding(padding).fillMaxSize()
        }
        
        Box(modifier = boxModifier) {
            when (uiState.currentSubScreen) {
                AppScreenEnum.SETTINGS -> SettingsMainScreen(
                    authManager = authManager,
                    settingsManager = settingsManager,
                    onNavigate = { viewModel.onEvent(SettingsEvent.Navigate(it)) }
                )
                AppScreenEnum.SETTINGS_ACCOUNT -> AccountScreen(
                    authManager = authManager,
                    settingsManager = settingsManager,
                    dataManager = dataManager,
                    onSignIn = onSignIn,
                    onSignOut = { 
                        onGroupChanged(null)
                        onGroupsUpdated(emptyMap())
                    }
                )
                AppScreenEnum.SETTINGS_GROUPS -> GroupsScreen(
                    dataManager = dataManager,
                    settingsManager = settingsManager,
                    onBack = { viewModel.onEvent(SettingsEvent.Navigate(AppScreenEnum.SETTINGS)) },
                    onGroupChanged = onGroupChanged,
                    onGroupsUpdated = onGroupsUpdated
                )
                AppScreenEnum.SETTINGS_GENERAL -> GeneralSettingsScreen(
                    settingsManager = settingsManager,
                    dataManager = dataManager,
                    onThemeToggle = onThemeToggle,
                    onLangToggle = onLangToggle
                )
                AppScreenEnum.SETTINGS_UPDATES -> AppUpdatesScreen(
                    backupManager = backupManager
                )
                AppScreenEnum.SETTINGS_VISIBILITY -> AppVisibilityScreen(
                    settingsManager = settingsManager,
                    onBack = { viewModel.onEvent(SettingsEvent.Navigate(AppScreenEnum.SETTINGS)) }
                )
                else -> {}
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    title: String, 
    subtitle: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
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
