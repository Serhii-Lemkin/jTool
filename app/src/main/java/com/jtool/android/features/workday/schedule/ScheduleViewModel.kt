package com.jtool.android.features.workday.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.workday.DaySchedule
import com.jtool.android.core.models.workday.WorkShift
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventStatusEnum
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale

class ScheduleViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        val options = settingsManager.getWorktimeOptions()
        val startDay = try { DayOfWeek.valueOf(options.weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }
        
        val now = LocalDate.now()
        var weekStart = now
        while (weekStart.dayOfWeek != startDay) {
            weekStart = weekStart.minusDays(1)
        }

        _uiState.update { it.copy(
            selectedWeekStart = weekStart,
            weeklyStartDay = startDay
        ) }
        
        loadWeekShifts(weekStart)
    }

    private fun loadWeekShifts(weekStart: LocalDate) {
        val userId = settingsManager.getUserId()
        val weekEnd = weekStart.plusDays(6)
        
        viewModelScope.launch {
            dataManager.getPrivateCalendarEventsFlow(userId, weekStart.toString(), weekEnd.toString())
                .take(1)
                .collect { events ->
                    val shifts = events.filter { it.type == CalendarEventTypeEnum.SHIFT }
                    val scheduleMap = mutableMapOf<DayOfWeek, DaySchedule>()
                    
                    DayOfWeek.entries.forEach { day ->
                        val dayOffset = (day.value - _uiState.value.weeklyStartDay.value + 7) % 7
                        val dayDate = weekStart.plusDays(dayOffset.toLong())
                        val dayShifts = shifts.filter { it.date == dayDate.toString() }
                            .map { WorkShift(it.startTime, it.endTime) }
                        
                        scheduleMap[day] = if (dayShifts.isNotEmpty()) {
                            DaySchedule(shifts = dayShifts, isConfirmedOff = false)
                        } else {
                            DaySchedule(shifts = emptyList(), isConfirmedOff = dayDate.isBefore(LocalDate.now()))
                        }
                    }
                    
                    _uiState.update { it.copy(
                        weeklySchedule = scheduleMap,
                        tempWeeklySchedule = scheduleMap,
                        hasUnsavedChanges = false
                    ) }
                }
        }
    }

    fun onEvent(event: ScheduleEvent) {
        when (event) {
            is ScheduleEvent.NavigateWeek -> {
                val newStart = _uiState.value.selectedWeekStart.plusWeeks(event.direction.toLong())
                _uiState.update { it.copy(selectedWeekStart = newStart, hasUnsavedChanges = false) }
                loadWeekShifts(newStart)
            }
            ScheduleEvent.CopyPrevWeek -> {
                // Future improvement: load previous week's shifts from DB and copy them
            }
            is ScheduleEvent.UpdateTempSchedule -> {
                val newTemp = _uiState.value.tempWeeklySchedule + (event.day to event.schedule)
                _uiState.update { it.copy(tempWeeklySchedule = newTemp, hasUnsavedChanges = true, conflictError = null, conflictingShifts = emptySet()) }
            }
            ScheduleEvent.Save -> {
                if (validateConflicts()) {
                    val current = _uiState.value
                    if (current.isLoading) return
                    
                    _uiState.update { it.copy(isLoading = true) }
                    
                    viewModelScope.launch {
                        val userId = settingsManager.getUserId()
                        val weekStart = current.selectedWeekStart
                        val weekEnd = weekStart.plusDays(6)

                        val newShifts = mutableListOf<CalendarEvent>()
                        current.tempWeeklySchedule.forEach { (day, schedule) ->
                            if (!schedule.isConfirmedOff) {
                                val dayOffset = (day.value - current.weeklyStartDay.value + 7) % 7
                                val dateStr = weekStart.plusDays(dayOffset.toLong()).toString()
                                
                                schedule.shifts.forEach { shift ->
                                    if (shift.start != null && shift.end != null) {
                                        newShifts.add(CalendarEvent(
                                            id = java.util.UUID.randomUUID().toString(),
                                            title = "Scheduled Shift",
                                            type = CalendarEventTypeEnum.SHIFT,
                                            date = dateStr,
                                            startTime = shift.start,
                                            endTime = shift.end,
                                            createdBy = userId,
                                            status = CalendarEventStatusEnum.REGULAR
                                        ))
                                    }
                                }
                            }
                        }

                        try {
                            dataManager.replacePrivateCalendarShifts(userId, weekStart.toString(), weekEnd.toString(), newShifts)
                            _uiState.update { it.copy(weeklySchedule = it.tempWeeklySchedule, hasUnsavedChanges = false, isLoading = false) }
                            dataManager.pushUserSettings()
                        } catch (e: Exception) {
                            android.util.Log.e("ScheduleVM", "Save failed", e)
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                }
            }
            is ScheduleEvent.ToggleTimePicker -> {
                _uiState.update { it.copy(
                    showTimePicker = event.show,
                    pickingDayForWeekly = event.day,
                    pickingShiftIndex = event.shiftIndex,
                    pickingStartTime = event.isStart
                ) }
            }
            is ScheduleEvent.SetTime -> {
                val day = _uiState.value.pickingDayForWeekly ?: return
                val index = _uiState.value.pickingShiftIndex ?: 0
                val currentSchedule = _uiState.value.tempWeeklySchedule[day] ?: DaySchedule()
                val shifts = currentSchedule.shifts.toMutableList()
                if (index >= shifts.size) {
                    shifts.add(WorkShift())
                }
                val shift = shifts[index]
                val newTime = String.format(Locale.ENGLISH, "%02d:%02d", event.time.hour, event.time.minute)
                val newShift = if (_uiState.value.pickingStartTime) shift.copy(start = newTime) else shift.copy(end = newTime)
                shifts[index] = newShift
                onEvent(ScheduleEvent.UpdateTempSchedule(day, currentSchedule.copy(shifts = shifts, isConfirmedOff = false)))
                _uiState.update { it.copy(showTimePicker = false) }
            }
            is ScheduleEvent.ToggleExitConfirm -> {
                _uiState.update { it.copy(showExitConfirm = event.show) }
            }
            is ScheduleEvent.AddShift -> {
                val current = _uiState.value.tempWeeklySchedule[event.day] ?: DaySchedule()
                val shifts = current.shifts.toMutableList()
                shifts.add(WorkShift("09:00", "17:00"))
                onEvent(ScheduleEvent.UpdateTempSchedule(event.day, current.copy(shifts = shifts, isConfirmedOff = false)))
            }
            is ScheduleEvent.RemoveShift -> {
                val current = _uiState.value.tempWeeklySchedule[event.day] ?: DaySchedule()
                val shifts = current.shifts.toMutableList()
                if (event.index < shifts.size) {
                    shifts.removeAt(event.index)
                }
                onEvent(ScheduleEvent.UpdateTempSchedule(event.day, current.copy(shifts = shifts)))
            }
            is ScheduleEvent.RequestScroll -> {
                _uiState.update { it.copy(scrollToShiftKey = event.shiftKey) }
            }
            ScheduleEvent.ClearScroll -> {
                _uiState.update { it.copy(scrollToShiftKey = null) }
            }
            ScheduleEvent.ClearError -> {
                _uiState.update { it.copy(conflictError = null, conflictingShifts = emptySet()) }
            }
        }
    }

    private fun validateConflicts(): Boolean {
        val schedule = _uiState.value.tempWeeklySchedule
        val days = DayOfWeek.entries
        val conflicts = mutableSetOf<String>()
        var error: String? = null
        
        for (i in 0 until 7) {
            val day = days[i]
            val nextDay = days[(i + 1) % 7]
            
            val dayShifts = schedule[day]?.shifts?.filter { it.start != null && it.end != null } ?: emptyList()
            val nextDayShifts = schedule[nextDay]?.shifts?.filter { it.start != null && it.end != null } ?: emptyList()
            
            // 1. Internal conflicts
            for (j in dayShifts.indices) {
                for (k in j + 1 until dayShifts.size) {
                    if (isOverlapping(dayShifts[j], dayShifts[k])) {
                        conflicts.add("${day.name}_$j")
                        conflicts.add("${day.name}_$k")
                        error = "Overlap on ${day.name}."
                    }
                }
            }
            
            // 2. Cross-day conflicts
            dayShifts.forEachIndexed { j, shift ->
                val startStr = shift.start ?: return@forEachIndexed
                val endStr = shift.end ?: return@forEachIndexed
                val start = CalendarEventHelper.parseTime(startStr)
                val end = CalendarEventHelper.parseTime(endStr)
                if (end.isBefore(start) || end == start) { // Ends next day
                    nextDayShifts.forEachIndexed { k, nextShift ->
                        val nextStartStr = nextShift.start ?: return@forEachIndexed
                        val nextStart = CalendarEventHelper.parseTime(nextStartStr)
                        if (nextStart.isBefore(end)) {
                            conflicts.add("${day.name}_$j")
                            conflicts.add("${nextDay.name}_$k")
                            error = "Shift on ${day.name} overlaps with ${nextDay.name}."
                        }
                    }
                }
            }
        }
        
        _uiState.update { it.copy(conflictError = error, conflictingShifts = conflicts) }
        return error == null
    }

    private fun isOverlapping(s1: WorkShift, s2: WorkShift): Boolean {
        val start1Str = s1.start ?: return false
        val end1Str = s1.end ?: return false
        val start2Str = s2.start ?: return false
        val end2Str = s2.end ?: return false

        val start1 = CalendarEventHelper.parseTime(start1Str)
        var end1 = CalendarEventHelper.parseTime(end1Str)
        if (end1.isBefore(start1) || end1 == start1) end1 = LocalTime.MAX
        
        val start2 = CalendarEventHelper.parseTime(start2Str)
        var end2 = CalendarEventHelper.parseTime(end2Str)
        if (end2.isBefore(start2) || end2 == start2) end2 = LocalTime.MAX
        
        return start1.isBefore(end2) && start2.isBefore(end1)
    }
}
