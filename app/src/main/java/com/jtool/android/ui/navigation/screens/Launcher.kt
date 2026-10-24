package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.ui.LauncherScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum
import kotlinx.coroutines.launch

object Launcher : AppProperties(AppScreenEnum.LAUNCHER) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        LauncherScreen(
            onWorkTimeClick = { deps.setScreen(AppScreenEnum.WORK_TIME) },
            onTimeCalcClick = { deps.setScreen(AppScreenEnum.TIME_CALC) },
            onShopListClick = { deps.setScreen(AppScreenEnum.SHOP_LIST) },
            onNotesClick = { deps.setScreen(AppScreenEnum.NOTES) },
            onLinksClick = { deps.setScreen(AppScreenEnum.LINKS) },
            onCleaningClick = { deps.setScreen(AppScreenEnum.CLEANING) },
            onTrendifyClick = { deps.setScreen(AppScreenEnum.TRENDIFY) },
            onCalendarClick = { deps.setScreen(AppScreenEnum.CALENDAR) },
            onVaultsClick = { deps.setScreen(AppScreenEnum.VAULTS) },
            onSettingsClick = { deps.setScreen(AppScreenEnum.SETTINGS) },
            onSupportClick = { deps.setScreen(AppScreenEnum.SUPPORT_HUB) },
            onAccountClick = { deps.setScreen(AppScreenEnum.SETTINGS_ACCOUNT) },
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            currentGroupId = deps.currentGroupId,
            groups = deps.groups,
            onJoinGroupClick = { 
                deps.setNavigateToGroups(true)
                deps.setScreen(AppScreenEnum.SETTINGS)
            },
            onGroupSelected = { id ->
                deps.onGroupChanged(id)
                deps.scope.launch { deps.dataManager.switchGroup(id) }
            },
            isAdmin = deps.isAdmin,
            onAdminClick = { deps.setScreen(AppScreenEnum.ADMIN_PANEL) },
            currentUser = deps.currentUser,
            notificationCount = deps.notificationCount,
            onNotificationsClick = deps.onNotificationsClick
        )
    }
}
