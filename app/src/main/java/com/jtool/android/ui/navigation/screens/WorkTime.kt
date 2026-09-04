package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.workday.tracker.WorkdayTrackerScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object WorkTime : AppProperties(
    AppScreenEnum.WORK_TIME,
    Icons.Default.Schedule,
    { Loc.Workday.workTime }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        WorkdayTrackerScreen(
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            onBack = { deps.onBack() },
            onNavigate = { deps.setScreen(it) }
        )
    }
}
