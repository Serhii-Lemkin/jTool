package com.jtool.android.features.workday.logconfig

import com.jtool.android.core.models.workday.DayLogHours
import java.time.DayOfWeek

sealed interface LogConfigEvent {
    data class UpdateTempSchedule(val day: DayOfWeek, val logHours: DayLogHours) : LogConfigEvent
    object Save : LogConfigEvent
    data class ToggleExitConfirm(val show: Boolean) : LogConfigEvent
    data class ToggleApproximateHoursPicker(val show: Boolean) : LogConfigEvent
    data class UpdateApproximateHoursConfig(
        val useStable: Boolean,
        val stableStart: String? = null,
        val stableEnd: String? = null
    ) : LogConfigEvent
    data class UpdateShowVirtualShiftsOnlyCurrentWeek(val show: Boolean) : LogConfigEvent
}
