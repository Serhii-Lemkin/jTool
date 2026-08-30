package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.cleaning.CleaningScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object Cleaning : AppProperties(
    AppScreenEnum.CLEANING,
    Icons.Default.Brush,
    { Loc.cleaning_cleaning }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        CleaningScreen(
            manager = deps.cleaningManager,
            backupManager = deps.backupManager,
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            onBack = { deps.setScreen(AppScreenEnum.LAUNCHER) }
        )
    }
}
