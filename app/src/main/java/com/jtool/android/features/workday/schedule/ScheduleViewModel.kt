package com.jtool.android.features.workday.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.workday.DaySchedule
import com.jtool.android.core.models.workday.WorkShift
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class ScheduleViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        val options = settingsManager.getWorktimeOptions()
        val startDay = try { DayOfWeek.valueOf(options.weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }
        val initialWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(startDay))
        _uiState.update { it.copy(weeklyStartDay = startDay, selectedWeekStart = initialWeekStart) }
        loadWeekShifts(initialWeekStart)
    }

    private fun loadWeekShifts(startOfWeek: LocalDate) {
        val displayedIds = settingsManager.getDisplayedCalendarIds()
        if (displayedIds.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val endOfWeek = startOfWeek.plusDays(6)
            val startMs = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = endOfWeek.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val events = GoogleCalendarSyncHelper.pullFromMultipleGoogleCalendars(dataManager.context, displayedIds, startMs, endMs)
            val dayEvents = events.groupBy { 
                val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.dtStart ?: 0L), ZoneId.systemDefault())
                ldt.dayOfWeek 
            }
            
            _uiState.update { it.copy(
                selectedWeekStart = startOfWeek,
                dayEvents = dayEvents,
                isLoading = false
            ) }
        }
    }

    fun onEvent(event: ScheduleEvent) {
        when (event) {
            is ScheduleEvent.NavigateWeek -> {
                val nextWeek = _uiState.value.selectedWeekStart.plusWeeks(event.direction.toLong())
                loadWeekShifts(nextWeek)
            }
            is ScheduleEvent.Save -> {
                saveShiftsToGoogle()
            }
            is ScheduleEvent.AddShift -> {
                val currentMap = _uiState.value.tempWeeklySchedule.toMutableMap()
                val currentSchedule = currentMap[event.day] ?: DaySchedule()
                val newShifts = currentSchedule.shifts.toMutableList()
                newShifts.add(WorkShift(start = "08:00", end = "16:30"))
                currentMap[event.day] = currentSchedule.copy(isConfirmedOff = false, shifts = newShifts)
                _uiState.update { it.copy(tempWeeklySchedule = currentMap, hasUnsavedChanges = true) }
            }
            is ScheduleEvent.RemoveShift -> {
                val currentMap = _uiState.value.tempWeeklySchedule.toMutableMap()
                val currentSchedule = currentMap[event.day] ?: DaySchedule()
                val newShifts = currentSchedule.shifts.toMutableList()
                if (event.index in newShifts.indices) {
                    newShifts.removeAt(event.index)
                    currentMap[event.day] = currentSchedule.copy(shifts = newShifts)
                    _uiState.update { it.copy(tempWeeklySchedule = currentMap, hasUnsavedChanges = true) }
                }
            }
            is ScheduleEvent.UpdateTempSchedule -> {
                val currentMap = _uiState.value.tempWeeklySchedule.toMutableMap()
                currentMap[event.day] = event.schedule
                _uiState.update { it.copy(tempWeeklySchedule = currentMap, hasUnsavedChanges = true) }
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
                val day = _uiState.value.pickingDayForWeekly
                val shiftIdx = _uiState.value.pickingShiftIndex
                val isStart = _uiState.value.pickingStartTime
                if (day != null && shiftIdx != null) {
                    val currentMap = _uiState.value.tempWeeklySchedule.toMutableMap()
                    val schedule = currentMap[day] ?: DaySchedule()
                    val shifts = schedule.shifts.toMutableList()
                    if (shiftIdx in shifts.indices) {
                        val shift = shifts[shiftIdx]
                        val timeStr = String.format(Locale.ENGLISH, "%02d:%02d", event.time.hour, event.time.minute)
                        val updatedShift = if (isStart) shift.copy(start = timeStr) else shift.copy(end = timeStr)
                        shifts[shiftIdx] = updatedShift
                        currentMap[day] = schedule.copy(shifts = shifts)
                        _uiState.update { it.copy(tempWeeklySchedule = currentMap, showTimePicker = false, hasUnsavedChanges = true) }
                    }
                }
            }
            ScheduleEvent.DismissSavedSuccess -> {
                _uiState.update { it.copy(showSavedSuccess = false) }
            }
            is ScheduleEvent.ToggleExitConfirm -> {
                _uiState.update { it.copy(showExitConfirm = event.show) }
            }
            ScheduleEvent.AbortExit -> {
                _uiState.update { it.copy(showExitConfirm = false) }
            }
            ScheduleEvent.CopyPrevWeek -> {
                _uiState.update { it.copy(tempWeeklySchedule = it.previousWeeklySchedule, hasUnsavedChanges = true) }
            }
            else -> {}
        }
    }

    private fun saveShiftsToGoogle() {
        val calendarId = settingsManager.getGoogleCalendarId()
            .ifEmpty {
                settingsManager.getDisplayedCalendarIds().firstOrNull()
                    ?: GoogleCalendarSyncHelper.getAvailableCalendars(dataManager.context).firstOrNull()?.first
                    ?: ""
            }
        if (calendarId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val weekStart = _uiState.value.selectedWeekStart
            val weekEnd = weekStart.plusDays(6)
            val workShiftColorKey = settingsManager.getWorkShiftColorKey()
            val sharedGids = emptyList<String>()

            val startMs = weekStart.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = weekEnd.plusDays(1).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val existingWeekEvents = GoogleCalendarSyncHelper.pullFromGoogleCalendar(dataManager.context, calendarId, startMs, endMs) ?: emptyList()
            existingWeekEvents.forEach { evt ->
                val hasJToolTag = evt.note.contains("jTool")
                val isDayOff = evt.title == Loc.Workday.dayOff || evt.title.contains("Day Off", ignoreCase = true)
                val isShift = evt.title == Loc.Calendar.shift || evt.title.contains("Shift", ignoreCase = true)
                val isWorkColor = evt.colorKey == workShiftColorKey
                if (hasJToolTag || (isWorkColor && (isDayOff || isShift))) {
                    GoogleCalendarSyncHelper.deleteFromGoogleCalendar(dataManager.context, evt.id)
                    evt.iCalUID?.let { dataManager.deleteCalendarExtension(it) }
                    dataManager.deleteCalendarExtension(evt.id)
                }
            }

            val startDay = _uiState.value.weeklyStartDay

            _uiState.value.tempWeeklySchedule.forEach { (day, schedule) ->
                val dayOffset = (day.value - startDay.value + 7) % 7
                val date = weekStart.plusDays(dayOffset.toLong())

                if (schedule.isConfirmedOff) {
                    try {
                        val startUtcMs = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                        val endUtcMs = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1L
                        val dayOffEvent = CalendarEvent(
                            title = Loc.Workday.dayOff,
                            dtStart = startUtcMs,
                            dtEnd = endUtcMs,
                            allDay = true,
                            colorKey = workShiftColorKey,
                            note = "jTool:work_schedule"
                        )
                        CalendarEventHelper.pushAndShareShiftOrEvent(
                            context = dataManager.context,
                            settingsManager = settingsManager,
                            dataManager = dataManager,
                            event = dayOffEvent,
                            calendarId = calendarId,
                            sharedGroupIds = sharedGids
                        )
                    } catch (_: Exception) {}
                } else if (schedule.shifts.isNotEmpty()) {
                    schedule.shifts.forEach { shift ->
                        if (shift.start != null) {
                            try {
                                val startLdt = date.atTime(LocalTime.parse(shift.start))
                                val endLdt = if (shift.end != null) date.atTime(LocalTime.parse(shift.end)) else startLdt.plusHours(8)
                                val baseNote = shift.note ?: ""
                                val noteWithTag = if (baseNote.contains("jTool")) baseNote else if (baseNote.isEmpty()) "jTool:work_schedule" else "$baseNote\njTool:work_schedule"

                                val event = CalendarEvent(
                                    title = shift.title ?: Loc.Calendar.shift,
                                    dtStart = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                    dtEnd = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                    colorKey = workShiftColorKey,
                                    note = noteWithTag
                                )
                                CalendarEventHelper.pushAndShareShiftOrEvent(
                                    context = dataManager.context,
                                    settingsManager = settingsManager,
                                    dataManager = dataManager,
                                    event = event,
                                    calendarId = calendarId,
                                    sharedGroupIds = sharedGids
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            _uiState.update { it.copy(isLoading = false, hasUnsavedChanges = false, showSavedSuccess = true) }
            loadWeekShifts(weekStart)
        }
    }
}
