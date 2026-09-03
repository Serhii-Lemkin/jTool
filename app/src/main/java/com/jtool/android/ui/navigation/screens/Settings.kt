package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.features.settings.SettingsScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object Settings : AppProperties(AppScreenEnum.SETTINGS) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        SettingsScreen(
            backupManager = deps.backupManager,
            settingsManager = deps.settingsManager,
            authManager = deps.authManager,
            dataManager = deps.dataManager,
            onSignIn = deps.onSignIn,
            isDarkMode = deps.isDarkMode,
            onThemeToggle = deps.onThemeToggle,
            currentLang = deps.currentLang,
            onLangToggle = deps.onLangToggle,
            startOnUpdates = deps.navigateToUpdates,
            startOnGroups = deps.navigateToGroups,
            onGroupChanged = deps.onGroupChanged,
            groups = deps.groups,
            onGroupsUpdated = deps.onGroupsUpdated,
            currentGroupId = deps.currentGroupId,
            onBack = { deps.setScreen(AppScreenEnum.LAUNCHER) }
        )
    }
}
