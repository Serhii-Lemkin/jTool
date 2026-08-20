package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.workday.WorkdayCalculatorScreen

object WorkTime : AppProperties(
    AppNames.WORK_TIME,
    Icons.Default.Schedule,
    { Loc.worktime_workTime }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        WorkdayCalculatorScreen(
            settingsManager = deps.settingsManager,
            groupSyncManager = deps.groupSyncManager,
            onBack = { deps.setScreen(AppNames.LAUNCHER) }
        )
    }
}
