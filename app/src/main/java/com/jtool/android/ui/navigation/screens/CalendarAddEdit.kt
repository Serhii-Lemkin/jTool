package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.calendar.edit.CalendarAddEditScreen
import com.jtool.android.features.calendar.grid.CalendarGridViewModel
import java.time.LocalDate

object CalendarAddEdit : AppProperties(AppScreenEnum.CALENDAR_ADD_EDIT) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val context = LocalContext.current
        val viewModel: CalendarGridViewModel = viewModel(
            viewModelStoreOwner = context as ComponentActivity,
            factory = viewModelFactory {
                initializer {
                    CalendarGridViewModel(context, deps.settingsManager, deps.dataManager)
                }
            }
        )
        
        val uiState by viewModel.uiState.collectAsState()
        
        LaunchedEffect(Unit) {
            if (uiState.editingEvent == null) {
                deps.settingsManager.getSelectedCalendarEventId()?.let { id ->
                    viewModel.prepareEdit(id)
                }
            }
        }

        val extension = uiState.editingEvent?.id?.let { uiState.extensions[it] }

        CalendarAddEditScreen(
            event = uiState.editingEvent,
            extension = extension,
            selectedDate = uiState.selectedDate,
            onBack = { deps.onBack() },
            onConfirm = { event, ext ->
                viewModel.addEvent(context, event, ext)
                deps.onBack()
            },
            settingsManager = deps.settingsManager,
            groups = deps.groups
        )
    }
}
