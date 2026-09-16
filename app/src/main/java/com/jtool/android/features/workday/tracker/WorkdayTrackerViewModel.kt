package com.jtool.android.features.workday.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.core.localization.Loc
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
            val displayedIds = settingsManager.getDisplayedCalendarIds()
            if (displayedIds.isNotEmpty()) {
                val today = LocalDate.now()
                val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMs = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val userEmail = settingsManager.getGoogleCalendarEmail()
                
                val events = GoogleCalendarSyncHelper.pullFromMultipleGoogleCalendars(dataManager.context, displayedIds, startMs, endMs, userEmail)
                _googleEvents.value = events
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
        _uiState.update { it.copy(
            trackingPreference = options.trackingPreference,
            logSchedule = options.logSchedule.mapKeys { DayOfWeek.valueOf(it.key) }
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
            // ... Logic for quick start timer
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
            else -> {}
        }
    }

    private fun processQuickStart() {
        viewModelScope.launch {
            val calendarId = settingsManager.getGoogleCalendarId()
            if (calendarId.isEmpty()) return@launch

            val options = settingsManager.getWorktimeOptions()
            val targetDurationMs = try {
                val start = LocalTime.parse(options.scheduleStart ?: "08:00")
                val end = LocalTime.parse(options.scheduleEnd ?: "16:30")
                Duration.between(start, end).toMillis()
            } catch (_: Exception) {
                (8.5 * 3600000).toLong()
            }

            val nowMs = System.currentTimeMillis()
            val plannedEndMs = nowMs + targetDurationMs

            val newEvent = CalendarEvent(
                title = Loc.Calendar.shift,
                dtStart = nowMs,
                dtEnd = plannedEndMs,
                note = "Quick Start Shift"
            )

            val createdId = GoogleCalendarSyncHelper.pushToGoogleCalendar(dataManager.context, newEvent, calendarId)
            _uiState.update { it.copy(quickStartEventId = createdId) }
            pullTodayGoogleEvents()
        }
    }

    private fun processFinish() {
        val state = _uiState.value
        if (!state.isSessionActive) return
        
        _uiState.update { it.copy(isSessionActive = false, selectedEventId = null, quickStartEventId = null) }

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
