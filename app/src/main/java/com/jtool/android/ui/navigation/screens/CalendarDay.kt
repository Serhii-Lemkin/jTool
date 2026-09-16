package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.features.calendar.day.CalendarDayScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.calendar.day.CalendarDayViewModel

object CalendarDay : AppProperties(AppScreenEnum.CALENDAR_DAY) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val calendarViewModel: CalendarDayViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    CalendarDayViewModel(deps.settingsManager, deps.dataManager)
                }
            }
        )

        CalendarDayScreen(
            onBack = deps.onBack,
            onNavigate = deps.setScreen,
            viewModel = calendarViewModel,
            groups = deps.groups
        )
    }
}
