package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.ui.LauncherScreen

object Launcher : AppProperties(AppNames.LAUNCHER) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        LauncherScreen(
            onWorkTimeClick = { deps.setScreen(AppNames.WORK_TIME) },
            onTimeCalcClick = { deps.setScreen(AppNames.TIME_CALC) },
            onShopListClick = { deps.setScreen(AppNames.SHOP_LIST) },
            onNotesClick = { deps.setScreen(AppNames.NOTES) },
            onLinksClick = { deps.setScreen(AppNames.LINKS) },
            onCleaningClick = { deps.setScreen(AppNames.CLEANING) },
            onSettingsClick = { deps.setScreen(AppNames.SETTINGS) },
            onAccountClick = { deps.setScreen(AppNames.SETTINGS_ACCOUNT) },
            showBadge = deps.hasUpdate || (deps.currentUser != null && deps.currentGroupId == null),
            settingsManager = deps.settingsManager,
            currentGroupId = deps.currentGroupId,
            groups = deps.groups,
            onJoinGroupClick = { 
                deps.setNavigateToGroups(true)
                deps.setScreen(AppNames.SETTINGS)
            },
            isAdmin = deps.isAdmin,
            onAdminClick = { deps.setScreen(AppNames.ADMIN_PANEL) },
            currentUser = deps.currentUser
        )
    }
}
