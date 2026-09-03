package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.calendar.grid.CalendarGridScreen
import com.jtool.android.features.calendar.grid.CalendarGridViewModel

object Calendar : AppProperties(
    id = AppScreenEnum.CALENDAR,
    icon = Icons.Default.CalendarMonth,
    getName = { Loc.calendar_calendar }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val calendarViewModel: CalendarGridViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    CalendarGridViewModel(deps.settingsManager, deps.dataManager)
                }
            }
        )

        CalendarGridScreen(
            onBack = deps.onBack,
            onNavigate = deps.setScreen,
            viewModel = calendarViewModel
        )
    }
}
