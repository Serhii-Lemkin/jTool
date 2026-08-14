package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.features.settings.SettingsScreen

object SettingsAccount : AppProperties(AppNames.SETTINGS_ACCOUNT) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        SettingsScreen(
            backupManager = deps.backupManager,
            settingsManager = deps.settingsManager,
            authManager = deps.authManager,
            groupSyncManager = deps.groupSyncManager,
            onSignIn = deps.onSignIn,
            isDarkMode = deps.isDarkMode,
            onThemeToggle = deps.onThemeToggle,
            currentLang = deps.currentLang,
            onLangToggle = deps.onLangToggle,
            hasUpdate = deps.hasUpdate,
            startOnUpdates = false,
            startOnGroups = false,
            startOnAccount = true,
            onGroupChanged = deps.onGroupChanged,
            groups = deps.groups,
            onGroupsUpdated = deps.onGroupsUpdated,
            currentGroupId = deps.currentGroupId,
            onBack = { deps.setScreen(AppNames.LAUNCHER) }
        )
    }
}
