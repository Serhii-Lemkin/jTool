package com.jtool.android.features.cleaning

import com.jtool.android.core.models.feature.cleaning.CleaningTask

sealed interface CleaningEvent {
    data class ToggleDisabledView(val show: Boolean) : CleaningEvent
    data class ToggleAddDialog(val show: Boolean) : CleaningEvent
    data class RequestDelete(val task: CleaningTask?) : CleaningEvent
    object ConfirmDelete : CleaningEvent
    data class ToggleItem(val task: CleaningTask, val isChecked: Boolean) : CleaningEvent
    data class UpdateTaskStatus(val task: CleaningTask, val isEnabled: Boolean) : CleaningEvent
    data class RequestEdit(val task: CleaningTask?) : CleaningEvent
    data class SaveTask(val title: String, val days: Int, val isRecurring: Boolean) : CleaningEvent
    data class AddTask(val title: String, val days: Int, val isRecurring: Boolean) : CleaningEvent
    object Refresh : CleaningEvent
}
