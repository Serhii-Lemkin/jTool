package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

        val editingEvent = uiState.editingEvent
        val extension = remember(editingEvent, uiState.extensions) {
            if (editingEvent == null) null
            else {
                uiState.extensions[editingEvent.id] 
                    ?: uiState.extensions[editingEvent.iCalUID] 
                    ?: uiState.extensions.values.find { it.googleEventId == editingEvent.id || it.googleEventId == editingEvent.iCalUID }
            }
        }

        val isReminder = editingEvent != null && editingEvent.reminderMinutes != null

        if (isReminder) {
            com.jtool.android.features.calendar.edit.CalendarReminderAddEditScreen(
                event = editingEvent,
                selectedDate = uiState.selectedDate,
                onBack = {
                    viewModel.clearEditingEvent()
                    deps.onBack()
                },
                onConfirm = { event, ext ->
                    viewModel.addEvent(context, event, ext)
                    viewModel.clearEditingEvent()
                    deps.onBack()
                },
                settingsManager = deps.settingsManager
            )
        } else {
            CalendarAddEditScreen(
                event = editingEvent,
                extension = extension,
                selectedDate = uiState.selectedDate,
                onBack = {
                    viewModel.clearEditingEvent()
                    deps.onBack()
                },
                onConfirm = { event, ext ->
                    viewModel.addEvent(context, event, ext)
                    viewModel.clearEditingEvent()
                    deps.onBack()
                },
                settingsManager = deps.settingsManager,
                groups = deps.groups
            )
        }
    }
}
