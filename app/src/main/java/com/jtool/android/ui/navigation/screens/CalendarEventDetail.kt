package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.calendar.detail.CalendarEventDetailScreen
import com.jtool.android.features.calendar.detail.CalendarEventDetailViewModel

object CalendarEventDetail : AppProperties(AppScreenEnum.CALENDAR_EVENT_DETAIL) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val calendarViewModel: CalendarEventDetailViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    CalendarEventDetailViewModel(deps.settingsManager, deps.dataManager)
                }
            }
        )

        CalendarEventDetailScreen(
            onBack = deps.onBack,
            viewModel = calendarViewModel
        )
    }
}
