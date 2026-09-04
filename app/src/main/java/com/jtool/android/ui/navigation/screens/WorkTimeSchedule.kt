package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.workday.schedule.ScheduleSettingsScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.workday.schedule.ScheduleViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

object WorkTimeSchedule : AppProperties(
    AppScreenEnum.WORK_TIME_SCHEDULE,
    null,
    { Loc.Workday.editWorkHours }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val viewModel: ScheduleViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    ScheduleViewModel(deps.settingsManager, deps.dataManager)
                }
            }
        )
        ScheduleSettingsScreen(
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            onBack = { deps.onBack() },
            viewModel = viewModel
        )
    }
}
