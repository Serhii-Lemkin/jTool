package com.jtool.android.features.workday.tracker

import android.provider.CalendarContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.core.localization.Loc
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.util.*

class WorkdayTrackerViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkdayTrackerUiState())
    val uiState: StateFlow<WorkdayTrackerUiState> = _uiState.asStateFlow()

    private val _googleEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private var tickerJob: Job? = null

    init {
        loadData()
        startTicker()
        pullTodayGoogleEvents()
        observeTodayEvents()
    }

    fun pullTodayGoogleEvents() {
        viewModelScope.launch {
            val calendarId = settingsManager.getGoogleCalendarId()
            val sharedCalendarId = settingsManager.getGoogleCalendarSharedId()
            val displayedIds = settingsManager.getDisplayedCalendarIds()
            if (displayedIds.isNotEmpty()) {
                val today = LocalDate.now()
                val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMs = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val userEmail = settingsManager.getGoogleCalendarEmail()
                
                val mainEvents = GoogleCalendarSyncHelper.pullFromMultipleGoogleCalendars(dataManager.context, displayedIds, startMs, endMs, userEmail)
                val allEvents = if (sharedCalendarId.isNotEmpty() && sharedCalendarId != calendarId) {
                    val sharedEvents = GoogleCalendarSyncHelper.pullFromGoogleCalendar(dataManager.context, sharedCalendarId, startMs, endMs, userEmail) ?: emptyList()
                    (mainEvents + sharedEvents).distinctBy { it.iCalUID ?: it.id }
                } else {
                    mainEvents
                }

                val extensions = dataManager.getAllCalendarExtensions()

                val processedEvents = allEvents.map { event ->
                    val ext = extensions[event.id] ?: extensions[event.iCalUID] ?: extensions.values.find { it.googleEventId == event.id || it.googleEventId == event.iCalUID }
                    val colorKey = event.colorKey?.takeIf { !it.isNullOrEmpty() } ?: ext?.colorKey ?: "7"
                    val title = if (!event.title.isNullOrEmpty() && event.title != Loc.Calendar.event) event.title else (ext?.title?.takeIf { !it.isNullOrEmpty() } ?: event.title.ifEmpty { Loc.Calendar.event })
                    event.copy(
                        colorKey = colorKey,
                        title = title
                    )
                }

                val existingIds = processedEvents.mapNotNull { it.iCalUID }.toSet() + processedEvents.map { it.id }.toSet()
                val extraEvents = mutableListOf<CalendarEvent>()
                extensions.forEach { (extId, ext) ->
                    if (!existingIds.contains(extId) && !existingIds.contains(ext.googleEventId) && ext.dateTimestamp != 0L) {
                        val eventDate = Instant.ofEpochMilli(ext.dateTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        if (eventDate == today) {
                            val authEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim() ?: ""
                            val extStatus = ext.attendeeStatuses[userEmail] ?: ext.attendeeStatuses[authEmail] ?: CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
                            extraEvents.add(
                                CalendarEvent(
                                    id = "ext_$extId",
                                    title = ext.title?.takeIf { it.isNotEmpty() } ?: "Shared Event",
                                    dtStart = ext.dateTimestamp,
                                    dtEnd = ext.dateTimestamp + 3600_000L,
                                    currentUserStatus = extStatus,
                                    iCalUID = ext.googleEventId,
                                    colorKey = ext.colorKey ?: "7"
                                )
                            )
                        }
                    }
                }

                _googleEvents.value = (processedEvents + extraEvents).sortedBy { it.dtStart ?: 0L }
            } else {
                _googleEvents.value = emptyList()
            }
        }
    }

    private fun observeTodayEvents() {
        viewModelScope.launch {
            _googleEvents.collectLatest { events ->
                val allTodayEvents = events.sortedBy { it.dtStart ?: 0L }
                
                val currentSelectedId = _uiState.value.selectedEventId
                val selectedExists = currentSelectedId != null && allTodayEvents.any { it.id == currentSelectedId || it.iCalUID == currentSelectedId }
                val validSelectedId = if (selectedExists) currentSelectedId else null

                if (allTodayEvents.isNotEmpty() && validSelectedId == null) {
                    val now = System.currentTimeMillis()
                    val closest = allTodayEvents.minByOrNull { event ->
                        val start = event.dtStart ?: 0L
                        Math.abs(start - now)
                    }
                    _uiState.update { it.copy(todayEvents = allTodayEvents, selectedEventId = closest?.id) }
                } else {
                    _uiState.update { it.copy(todayEvents = allTodayEvents, selectedEventId = validSelectedId) }
                }
            }
        }
    }

    private fun loadData() {
        val options = settingsManager.getWorktimeOptions()
        val now = LocalDateTime.now()
        _uiState.update { it.copy(
            trackingPreference = options.trackingPreference,
            logSchedule = options.logSchedule.mapKeys { DayOfWeek.valueOf(it.key) },
            freehandStart = now,
            freehandEnd = now.plusHours(8),
            freehandDurationHours = 8,
            freehandDurationMinutes = 30
        ) }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                calculateRealTimeData()
                delay(1000)
            }
        }
    }

    private fun calculateRealTimeData() {
        val state = _uiState.value
        if (!state.isSessionActive) return

        val now = System.currentTimeMillis()
        
        if (state.mode == WorkModeEnum.QUICK_START) {
            val start = state.quickStartStartMs
            val end = state.quickStartEndMs
            if (start > 0L && end > 0L) {
                val isFinished = now > end
                val isNotStarted = now < start
                
                val elapsed = (now - start).coerceAtLeast(0)
                val duration = (end - start).coerceAtLeast(1)
                
                _uiState.update { it.copy(
                    quickStartTimer = TimerState(
                        timeLeftMillis = if (isNotStarted) (start - now) else (end - now).coerceAtLeast(0),
                        overtimeMillis = if (isFinished) (now - end) else 0L,
                        progress = (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                        isFinished = isFinished,
                        isNotStarted = isNotStarted,
                        label = when {
                            isFinished -> Loc.Workday.overtime
                            isNotStarted -> Loc.Workday.untilStart
                            else -> Loc.Workday.finishAt
                        }
                    )
                ) }
            }
        } else {
            val selectedEvent = _googleEvents.value.find { it.id == state.selectedEventId }
            if (selectedEvent != null) {
                val start = selectedEvent.dtStart ?: 0L
                val end = selectedEvent.dtEnd ?: Long.MAX_VALUE
                
                val isFinished = now > end
                val isNotStarted = now < start
                
                val elapsed = (now - start).coerceAtLeast(0)
                val duration = (end - start).coerceAtLeast(1)
                
                _uiState.update { it.copy(
                    eventTimer = TimerState(
                        timeLeftMillis = if (isNotStarted) (start - now) else (end - now).coerceAtLeast(0),
                        overtimeMillis = if (isFinished) (now - end) else 0L,
                        progress = (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                        isFinished = isFinished,
                        isNotStarted = isNotStarted,
                        label = when {
                            isFinished -> Loc.Workday.overtime
                            isNotStarted -> Loc.Workday.untilStart
                            else -> Loc.Workday.finishAt
                        }
                    )
                ) }
            }
        }
    }

    fun onEvent(event: WorkdayTrackerEvent) {
        when (event) {
            is WorkdayTrackerEvent.ChangeMode -> {
                _uiState.update { it.copy(mode = event.mode) }
            }
            is WorkdayTrackerEvent.SelectEvent -> {
                _uiState.update { it.copy(selectedEventId = event.eventId, isSessionActive = event.eventId != null) }
            }
            is WorkdayTrackerEvent.StartSession -> {
                _uiState.update { it.copy(isSessionActive = true) }
                if (_uiState.value.mode == WorkModeEnum.QUICK_START) {
                    processQuickStart()
                }
            }
            is WorkdayTrackerEvent.Refresh -> {
                pullTodayGoogleEvents()
            }
            is WorkdayTrackerEvent.FinishSession -> {
                processFinish()
            }
            is WorkdayTrackerEvent.ResetSession -> {
                _uiState.update { it.copy(isSessionActive = false, selectedEventId = null, quickStartEventId = null, quickStartStartMs = 0L, quickStartEndMs = 0L, showResetConfirm = false) }
            }
            is WorkdayTrackerEvent.SetFreehandStart -> {
                _uiState.update { it.copy(freehandStart = event.dateTime) }
            }
            is WorkdayTrackerEvent.SetFreehandEnd -> {
                _uiState.update { it.copy(freehandEnd = event.dateTime) }
            }
            is WorkdayTrackerEvent.SetFreehandDuration -> {
                _uiState.update { it.copy(freehandDurationHours = event.hours, freehandDurationMinutes = event.minutes) }
            }
            is WorkdayTrackerEvent.SetUseDuration -> {
                _uiState.update { it.copy(isUsingDuration = event.use) }
            }
            is WorkdayTrackerEvent.TogglePicker -> {
                _uiState.update { 
                    it.copy(
                        showDatePicker = if (event.picker == PickerTypeEnum.DATE) event.show else it.showDatePicker,
                        showTimePicker = if (event.picker == PickerTypeEnum.TIME) event.show else it.showTimePicker,
                        pickingStartTime = event.isStart,
                        isPickingWeeklyToday = event.forWeeklyToday
                    ) 
                }
            }
            is WorkdayTrackerEvent.ToggleResetConfirm -> {
                _uiState.update { it.copy(showResetConfirm = event.show) }
            }
            is WorkdayTrackerEvent.ToggleStartConfirm -> {
                _uiState.update { it.copy(showStartConfirm = event.show) }
            }
            is WorkdayTrackerEvent.ToggleQuickStartError -> {
                _uiState.update { it.copy(showQuickStartConfigError = event.show) }
            }
            is WorkdayTrackerEvent.SetWeeklyTodayStart -> {
                _uiState.update { it.copy(weeklyTodayStart = event.time) }
            }
            is WorkdayTrackerEvent.SetWeeklyTodayEnd -> {
                _uiState.update { it.copy(weeklyTodayEnd = event.time) }
            }
            is WorkdayTrackerEvent.ToggleWeeklyAdjustment -> {
                _uiState.update { it.copy(isWeeklyAdjustmentActive = event.active) }
            }
            else -> {}
        }
    }

    private fun processQuickStart() {
        viewModelScope.launch {
            val calendarId = settingsManager.getGoogleCalendarId()
            if (calendarId.isEmpty()) return@launch

            val state = _uiState.value
            val startMs = state.freehandStart?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
            
            val durationMs = if (state.isUsingDuration) {
                (state.freehandDurationHours * 3600L + state.freehandDurationMinutes * 60L) * 1000L
            } else {
                val endMsVal = state.freehandEnd?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: (startMs + 8 * 3600000L)
                (endMsVal - startMs).coerceAtLeast(0L)
            }

            val finalDurationMs = if (durationMs <= 0L) (8.5 * 3600000).toLong() else durationMs
            val plannedEndMs = startMs + finalDurationMs

            _uiState.update { it.copy(
                quickStartStartMs = startMs,
                quickStartEndMs = plannedEndMs
            ) }

            val workShiftColorKey = settingsManager.getWorkShiftColorKey()
            val newEvent = CalendarEvent(
                title = Loc.Calendar.shift,
                dtStart = startMs,
                dtEnd = plannedEndMs,
                note = "Quick Start Shift",
                colorKey = workShiftColorKey
            )

            val createdId = GoogleCalendarSyncHelper.pushToGoogleCalendar(dataManager.context, newEvent, calendarId)
            _uiState.update { it.copy(quickStartEventId = createdId) }
            pullTodayGoogleEvents()
        }
    }

    private fun processFinish() {
        val state = _uiState.value
        if (!state.isSessionActive) return
        
        _uiState.update { it.copy(isSessionActive = false, selectedEventId = null, quickStartEventId = null, quickStartStartMs = 0L, quickStartEndMs = 0L) }

        viewModelScope.launch {
            val nowMs = System.currentTimeMillis()
            val calendarId = settingsManager.getGoogleCalendarId()
            
            if (calendarId.isNotEmpty()) {
                val targetEventId = if (state.mode == WorkModeEnum.QUICK_START) state.quickStartEventId else state.selectedEventId
                val existing = _googleEvents.value.find { it.id == targetEventId || it.iCalUID == targetEventId }
                
                if (existing != null) {
                    val updated = existing.copy(
                        dtEnd = nowMs,
                        note = "${existing.note}\nFinished: ${LocalTime.now()}".trim()
                    )
                    GoogleCalendarSyncHelper.pushToGoogleCalendar(dataManager.context, updated, calendarId)
                } else if (state.mode == WorkModeEnum.QUICK_START && targetEventId != null) {
                    val updated = CalendarEvent(
                        id = targetEventId,
                        title = Loc.Calendar.shift,
                        dtStart = System.currentTimeMillis() - 8 * 3600000,
                        dtEnd = nowMs,
                        note = "Quick Start Shift"
                    )
                    GoogleCalendarSyncHelper.pushToGoogleCalendar(dataManager.context, updated, calendarId)
                }
            }

            pullTodayGoogleEvents()
        }
    }
}
