package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.features.admin.AdminPanelScreen

object AdminPanel : AppProperties(AppNames.ADMIN_PANEL) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        AdminPanelScreen(
            groupSyncManager = deps.groupSyncManager,
            onBack = { deps.setScreen(AppNames.LAUNCHER) }
        )
    }
}
