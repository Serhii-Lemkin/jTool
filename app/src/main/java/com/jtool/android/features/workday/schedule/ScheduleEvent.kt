package com.jtool.android.features.workday.schedule

import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.workday.DaySchedule
import java.time.DayOfWeek
import java.time.LocalTime

sealed interface ScheduleEvent {
    data class NavigateWeek(val direction: Int) : ScheduleEvent
    data class UpdateTempSchedule(val day: DayOfWeek, val schedule: DaySchedule) : ScheduleEvent
    object CopyPrevWeek : ScheduleEvent
    object Save : ScheduleEvent
    data class ToggleTimePicker(
        val show: Boolean, 
        val day: DayOfWeek? = null, 
        val shiftIndex: Int? = null,
        val isStart: Boolean = true
    ) : ScheduleEvent
    data class SetTime(val time: LocalTime) : ScheduleEvent
    data class ToggleExitConfirm(val show: Boolean) : ScheduleEvent
    object AbortExit : ScheduleEvent
    data class AddShift(val day: DayOfWeek) : ScheduleEvent
    data class RemoveShift(val day: DayOfWeek, val index: Int) : ScheduleEvent
    data class RequestScroll(val shiftKey: String) : ScheduleEvent
    object ClearScroll : ScheduleEvent
    object ClearError : ScheduleEvent
    object DismissSavedSuccess : ScheduleEvent
    data class ToggleGoogleSync(val enabled: Boolean) : ScheduleEvent
    data class ConvertEventToShift(val day: DayOfWeek, val event: CalendarEvent) : ScheduleEvent
}
