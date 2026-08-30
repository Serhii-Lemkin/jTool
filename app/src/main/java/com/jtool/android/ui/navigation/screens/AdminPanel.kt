package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.features.admin.AdminPanelScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object AdminPanel : AppProperties(AppScreenEnum.ADMIN_PANEL) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        AdminPanelScreen(
            dataManager = deps.dataManager,
            onBack = { deps.setScreen(AppScreenEnum.LAUNCHER) }
        )
    }
}
