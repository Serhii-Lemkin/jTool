package com.jtool.android.features.workday.logconfig

import com.jtool.android.core.models.workday.DayLogHours
import java.time.DayOfWeek

data class LogConfigUiState(
    val weeklySchedule: Map<DayOfWeek, DayLogHours> = emptyMap(),
    val tempWeeklySchedule: Map<DayOfWeek, DayLogHours> = emptyMap(),
    val weeklyStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val hasUnsavedChanges: Boolean = false,
    val showExitConfirm: Boolean = false,
    val isLoading: Boolean = false,
    val showApproximateHoursPicker: Boolean = false,
    
    val useStableApproximateHours: Boolean = true,
    val stableApproximateStart: String? = null,
    val stableApproximateEnd: String? = null,

    // Pending changes in the bottom sheet
    val pendingUseStable: Boolean = true,
    val pendingStableStart: String? = null,
    val pendingStableEnd: String? = null,
    val pendingWeeklySchedule: Map<DayOfWeek, DayLogHours> = emptyMap(),
    
    val showVirtualShiftsOnlyCurrentWeek: Boolean = false
)
