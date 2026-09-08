package com.jtool.android.features.workday.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.workday.DaySchedule
import com.jtool.android.core.models.workday.WorkShift
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventStatusEnum
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

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
            weeklyStartDay = startDay,
            syncToGoogle = settingsManager.isGoogleCalendarPushEnabled()
        ) }
        
        loadWeekShifts(weekStart)
    }

    private fun loadWeekShifts(weekStart: LocalDate) {
        val weekEnd = weekStart.plusDays(6)
        
        val startMs = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = weekEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val googleEvents = if (settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPullEnabled()) {
                val calendarId = settingsManager.getGoogleCalendarId()
                if (calendarId.isNotEmpty() && calendarId != "primary") {
                    GoogleCalendarSyncHelper.pullFromGoogleCalendar(dataManager.context, settingsManager.getUserId(), calendarId, startMs, endMs)
                } else null
            } else null
            
            val firestoreEvents = dataManager.allCalendarEventsOneTime(startMs, endMs)
            val allEvents = (firestoreEvents + (googleEvents ?: emptyList())).distinctBy { it.googleEventId ?: it.id }
            
            val shifts = allEvents.filter { it.type == CalendarEventTypeEnum.SHIFT }
            val scheduleMap = mutableMapOf<DayOfWeek, DaySchedule>()
            val dayEventsMap = mutableMapOf<DayOfWeek, List<CalendarEvent>>()
            
            DayOfWeek.entries.forEach { day ->
                val dayOffset = (day.value - _uiState.value.weeklyStartDay.value + 7) % 7
                val dayDate = weekStart.plusDays(dayOffset.toLong())
                
                val dayShifts = shifts.filter { it.date == dayDate.toString() }
                    .map { WorkShift(it.startTime, it.endTime, it.title, it.note) }
                
                val dayOthers = allEvents.filter { it.date == dayDate.toString() && it.type != CalendarEventTypeEnum.SHIFT }
                dayEventsMap[day] = dayOthers
                
                scheduleMap[day] = if (dayShifts.isNotEmpty()) {
                    DaySchedule(shifts = dayShifts, isConfirmedOff = false)
                } else {
                    DaySchedule(shifts = emptyList(), isConfirmedOff = dayDate.isBefore(LocalDate.now()))
                }
            }
            
            _uiState.update { it.copy(
                weeklySchedule = scheduleMap,
                tempWeeklySchedule = scheduleMap,
                dayEvents = dayEventsMap,
                hasUnsavedChanges = false,
                isLoading = false
            ) }
        }
    }

    fun onEvent(event: ScheduleEvent) {
        when (event) {
            is ScheduleEvent.NavigateWeek -> {
                if (_uiState.value.hasUnsavedChanges) {
                    _uiState.update { it.copy(showExitConfirm = true, pendingNavigateWeek = event.direction) }
                } else {
                    val newStart = _uiState.value.selectedWeekStart.plusWeeks(event.direction.toLong())
                    _uiState.update { it.copy(selectedWeekStart = newStart, hasUnsavedChanges = false) }
                    loadWeekShifts(newStart)
                }
            }
            ScheduleEvent.CopyPrevWeek -> {
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

                        val startMs = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val endMs = weekEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                        dataManager.clearCalendarShifts(userId, emptyList(), startMs, endMs)

                        current.tempWeeklySchedule.forEach { (day, schedule) ->
                            if (!schedule.isConfirmedOff) {
                                val dayOffset = (day.value - current.weeklyStartDay.value + 7) % 7
                                val dateStr = weekStart.plusDays(dayOffset.toLong()).toString()
                                
                                schedule.shifts.forEach { shift ->
                                    if (shift.start != null && shift.end != null) {
                                        val startLdt = LocalDate.parse(dateStr).atTime(CalendarEventHelper.parseTime(shift.start))
                                        val endLdt = LocalDate.parse(dateStr).atTime(CalendarEventHelper.parseTime(shift.end))
                                        
                                        val cleanNote = shift.note?.substringBefore("\n\n--- jTool Data ---")?.trim() ?: ""
                                        val finalNote = if (cleanNote.isEmpty()) "Type: SHIFT" else "$cleanNote\nType: SHIFT"

                                        var e = CalendarEvent(
                                            id = UUID.randomUUID().toString(),
                                            title = shift.title ?: Loc.Calendar.shift,
                                            type = CalendarEventTypeEnum.SHIFT,
                                            date = dateStr,
                                            startTime = shift.start,
                                            endTime = shift.end,
                                            createdBy = "jTool",
                                            ownerUid = userId,
                                            status = CalendarEventStatusEnum.REGULAR,
                                            dtStart = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                            dtEnd = if (endLdt.isBefore(startLdt)) endLdt.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() 
                                                    else endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                            note = finalNote
                                        )

                                        if (current.syncToGoogle) {
                                            val gCalendarId = settingsManager.getGoogleCalendarId()
                                            if (gCalendarId.isNotEmpty() && gCalendarId != "primary") {
                                                try {
                                                    val gId = GoogleCalendarSyncHelper.pushToGoogleCalendar(
                                                        dataManager.context,
                                                        e,
                                                        gCalendarId
                                                    )
                                                    if (gId != null) {
                                                        e = e.copy(googleEventId = gId.toString(), googleCalendarId = gCalendarId)
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        if (e.googleEventId != null) {
                                            dataManager.deleteCalendarEventLocalOnly(e.id)
                                        } else {
                                            dataManager.saveCalendarEvent(e)
                                        }
                                    }
                                }
                            }
                        }

                        _uiState.update { it.copy(weeklySchedule = it.tempWeeklySchedule, hasUnsavedChanges = false, isLoading = false) }
                        dataManager.pushUserSettings()
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
                if (!event.show && _uiState.value.pendingNavigateWeek != null) {
                    val direction = _uiState.value.pendingNavigateWeek!!
                    val newStart = _uiState.value.selectedWeekStart.plusWeeks(direction.toLong())
                    _uiState.update { it.copy(selectedWeekStart = newStart, hasUnsavedChanges = false, showExitConfirm = false, pendingNavigateWeek = null) }
                    loadWeekShifts(newStart)
                } else {
                    _uiState.update { it.copy(showExitConfirm = event.show) }
                }
            }
            ScheduleEvent.AbortExit -> {
                _uiState.update { it.copy(showExitConfirm = false, pendingNavigateWeek = null) }
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
            is ScheduleEvent.ToggleGoogleSync -> {
                _uiState.update { it.copy(syncToGoogle = event.enabled) }
            }
            is ScheduleEvent.ConvertEventToShift -> {
                val current = _uiState.value.tempWeeklySchedule[event.day] ?: DaySchedule()
                val shifts = current.shifts.toMutableList()
                shifts.add(WorkShift(event.event.startTime, event.event.endTime, event.event.title, event.event.note))
                onEvent(ScheduleEvent.UpdateTempSchedule(event.day, current.copy(shifts = shifts, isConfirmedOff = false)))
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
            
            for (j in dayShifts.indices) {
                for (k in j + 1 until dayShifts.size) {
                    if (isOverlapping(dayShifts[j], dayShifts[k])) {
                        conflicts.add("${day.name}_$j")
                        conflicts.add("${day.name}_$k")
                        error = "Overlap on ${day.name}."
                    }
                }
            }
            
            dayShifts.forEachIndexed { j, shift ->
                val startStr = shift.start ?: return@forEachIndexed
                val endStr = shift.end ?: return@forEachIndexed
                val start = CalendarEventHelper.parseTime(startStr)
                val end = CalendarEventHelper.parseTime(endStr)
                if (end.isBefore(start) || end == start) {
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
