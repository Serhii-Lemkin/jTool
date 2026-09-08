package com.jtool.android.features.workday.tracker

import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import java.time.LocalDateTime
import java.time.LocalTime

sealed interface WorkdayTrackerEvent {
    data class ChangeMode(val mode: WorkModeEnum) : WorkdayTrackerEvent
    data class SetTrackingPreference(val preference: WorkTrackingPreferenceEnum) : WorkdayTrackerEvent
    
    // Quick Start
    data class SetFreehandStart(val dateTime: LocalDateTime) : WorkdayTrackerEvent
    data class SetFreehandEnd(val dateTime: LocalDateTime) : WorkdayTrackerEvent
    data class SetFreehandDuration(val hours: Int, val minutes: Int) : WorkdayTrackerEvent
    data class SetUseDuration(val use: Boolean) : WorkdayTrackerEvent
    
    // Schedule Adjustment
    data class SetWeeklyTodayStart(val time: LocalTime) : WorkdayTrackerEvent
    data class SetWeeklyTodayEnd(val time: LocalTime) : WorkdayTrackerEvent
    data class ToggleWeeklyAdjustment(val active: Boolean) : WorkdayTrackerEvent
    
    // Global Session
    object StartSession : WorkdayTrackerEvent
    object ConfirmStartSession : WorkdayTrackerEvent
    object ResetSession : WorkdayTrackerEvent
    object FinishSession : WorkdayTrackerEvent
    
    // UI Helpers
    data class TogglePicker(val picker: PickerTypeEnum, val show: Boolean, val isStart: Boolean = true, val forWeeklyToday: Boolean = false) : WorkdayTrackerEvent
    data class ToggleResetConfirm(val show: Boolean) : WorkdayTrackerEvent
    data class ToggleStartConfirm(val show: Boolean) : WorkdayTrackerEvent
    data class ToggleQuickStartError(val show: Boolean) : WorkdayTrackerEvent
    object Refresh : WorkdayTrackerEvent

    data class SelectEvent(val eventId: String?) : WorkdayTrackerEvent
    object StartNow : WorkdayTrackerEvent
    object StartCountdown : WorkdayTrackerEvent
}
