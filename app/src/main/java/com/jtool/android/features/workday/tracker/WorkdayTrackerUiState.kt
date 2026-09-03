package com.jtool.android.features.workday.tracker

import com.jtool.android.core.models.workday.DayLogHours
import com.jtool.android.core.models.workday.DaySchedule
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

data class WorkdayTrackerUiState(
    val mode: WorkModeEnum = WorkModeEnum.SCHEDULE,
    val trackingPreference: WorkTrackingPreferenceEnum? = null,
    val logSchedule: Map<DayOfWeek, DayLogHours> = emptyMap(),
    val weeklyStartDay: DayOfWeek = DayOfWeek.MONDAY,
    
    // Quick Start
    val freehandStart: LocalDateTime? = null,
    val freehandEnd: LocalDateTime? = null,
    val freehandDurationHours: Int = 8,
    val freehandDurationMinutes: Int = 0,
    val isUsingDuration: Boolean = false,
    
    // Log Hours session
    val logHoursSessionStart: LocalDateTime? = null,
    val isLogHoursSet: Boolean = false,
    
    // Schedule session
    val scheduleStart: LocalDateTime? = null,
    val scheduleEnd: LocalDateTime? = null,
    
    // Weekly Adjustment
    val weeklyTodayStart: LocalTime? = null,
    val weeklyTodayEnd: LocalTime? = null,
    val isWeeklyAdjustmentActive: Boolean = false,
    
    // Session Calculations
    val timeLeftMillis: Long = 0L,
    val overtimeMillis: Long = 0L,
    val progress: Float = 0f,
    val isFinished: Boolean = false,
    val isSessionActive: Boolean = false,
    val activeSessionMode: WorkModeEnum? = null,
    val activeEventId: String? = null,
    val noHoursScheduled: Boolean = false,
    
    // UI State
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showResetConfirm: Boolean = false,
    val showStartConfirm: Boolean = false,
    val showQuickStartConfigError: Boolean = false,
    val pickingStartTime: Boolean = true,
    val isPickingWeeklyToday: Boolean = false,
    val errorText: String = "",
    val isLoading: Boolean = false,
    val todayCalendarShift: com.jtool.android.core.models.calendar.CalendarEvent? = null
)
