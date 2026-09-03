package com.jtool.android.features.workday.schedule

import com.jtool.android.core.models.workday.DaySchedule
import java.time.DayOfWeek
import java.time.LocalDate

data class ScheduleUiState(
    val selectedWeekStart: LocalDate = LocalDate.now(),
    val weeklySchedule: Map<DayOfWeek, DaySchedule> = emptyMap(),
    val tempWeeklySchedule: Map<DayOfWeek, DaySchedule> = emptyMap(),
    val previousWeeklySchedule: Map<DayOfWeek, DaySchedule> = emptyMap(),
    val weeklyStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val currentWeekString: String = "",
    val hasUnsavedChanges: Boolean = false,
    val showExitConfirm: Boolean = false,
    val isLoading: Boolean = false,
    val pickingStartTime: Boolean = true,
    val pickingDayForWeekly: DayOfWeek? = null,
    val pickingShiftIndex: Int? = null,
    val showTimePicker: Boolean = false,
    val conflictError: String? = null,
    val conflictingShifts: Set<String> = emptySet(), // Format: "DAY_INDEX"
    val scrollToShiftKey: String? = null // Format: "DAY_INDEX"
)
