package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.features.support.screens.SupportHubScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object SupportHub : AppProperties(AppScreenEnum.SUPPORT_HUB) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        SupportHubScreen(
            dataManager = deps.dataManager,
            settingsManager = deps.settingsManager,
            onBack = { deps.onBack() }
        )
    }
}
