package com.jtool.android.features.workday

import java.time.DayOfWeek

sealed interface WorkdayEvent {
    data class UpdateInputTime(val time: String) : WorkdayEvent
    object Calculate : WorkdayEvent
    object ToggleRemember : WorkdayEvent
    data class ToggleForgetConfirm(val show: Boolean) : WorkdayEvent
    object ConfirmForget : WorkdayEvent
    data class ToggleEditDialog(val show: Boolean) : WorkdayEvent
    data class SaveWeeklyHours(val hours: Map<DayOfWeek, Double>) : WorkdayEvent
}
