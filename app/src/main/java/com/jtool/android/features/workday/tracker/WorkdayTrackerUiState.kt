package com.jtool.android.features.workday.tracker

import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.workday.DayLogHours
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

data class TimerState(
    val timeLeftMillis: Long = 0L,
    val overtimeMillis: Long = 0L,
    val progress: Float = 0f,
    val isFinished: Boolean = false,
    val label: String? = null
)

data class WorkdayTrackerUiState(
    val mode: WorkModeEnum = WorkModeEnum.CALENDAR_EVENT,
    val trackingPreference: WorkTrackingPreferenceEnum? = null,
    val logSchedule: Map<DayOfWeek, DayLogHours> = emptyMap(),
    val weeklyStartDay: DayOfWeek = DayOfWeek.MONDAY,
    
    // Quick Start Settings
    val freehandStart: LocalDateTime? = null,
    val freehandEnd: LocalDateTime? = null,
    val freehandDurationHours: Int = 0,
    val freehandDurationMinutes: Int = 0,
    val isUsingDuration: Boolean = true,
    
    // Weekly Today Adjustment
    val weeklyTodayStart: LocalTime? = null,
    val weeklyTodayEnd: LocalTime? = null,
    val isWeeklyAdjustmentActive: Boolean = false,
    
    // Timers
    val quickStartTimer: TimerState = TimerState(),
    val eventTimer: TimerState = TimerState(),
    
    // Active Session State
    val isSessionActive: Boolean = false,
    val activeSessionMode: WorkModeEnum? = null,
    val activeEventId: String? = null,
    
    // Legacy / Shared
    val timeLeftMillis: Long = 0L,
    val overtimeMillis: Long = 0L,
    val progress: Float = 0f,
    val isFinished: Boolean = false,
    
    // UI Controls
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showResetConfirm: Boolean = false,
    val showStartConfirm: Boolean = false,
    val showQuickStartConfigError: Boolean = false,
    val pickingStartTime: Boolean = true,
    val isPickingWeeklyToday: Boolean = false,
    val errorText: String = "",
    val isLoading: Boolean = false,
    val todayRequiredHours: Int = 0,
    val todayRequiredMinutes: Int = 0,
    val todayCalendarShift: CalendarEvent? = null,

    val todayEvents: List<CalendarEvent> = emptyList(),
    val selectedEventId: String? = null,
    val isCountingDown: Boolean = false,
    val countdownLabel: String? = null
)
