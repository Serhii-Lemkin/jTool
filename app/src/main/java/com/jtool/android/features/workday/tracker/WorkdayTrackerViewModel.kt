package com.jtool.android.features.workday.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.workday.DayLogHours
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventStatusEnum
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.workday.WorkHistoryItem
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

class WorkdayTrackerViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkdayTrackerUiState())
    val uiState: StateFlow<WorkdayTrackerUiState> = _uiState.asStateFlow()

    private val _googleEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private var tickerJob: Job? = null

    init {
        loadData()
        startTicker()
        observeTodayEvents()
        pullTodayGoogleEvents()
    }

    private fun pullTodayGoogleEvents() {
        if (!settingsManager.isGoogleCalendarSyncEnabled() || !settingsManager.isGoogleCalendarPullEnabled()) return
        
        val calendarId = settingsManager.getGoogleCalendarId()
        if (calendarId.isEmpty() || calendarId == "primary") return

        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val events = GoogleCalendarSyncHelper.pullFromGoogleCalendar(
                dataManager.context, 
                settingsManager.getUserId(), 
                calendarId, 
                startMs, 
                endMs
            )
            _googleEvents.value = events ?: emptyList()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTodayEvents() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            combine(dataManager.allCalendarEventsFlow(startMs, endMs), _googleEvents) { firestoreEvents, googleEvents ->
                (firestoreEvents + googleEvents).distinctBy { it.googleEventId ?: it.id }
            }.collectLatest { events ->
                val sorted = events.sortedBy { it.dtStart ?: CalendarEventHelper.parseTime(it.startTime).toNanoOfDay() }
                
                _uiState.update { it.copy(todayEvents = sorted) }

                if (_uiState.value.selectedEventId == null) {
                    val now = LocalTime.now()
                    
                    // Selection Logic:
                    // 1. If a Shift is currently happening, select it.
                    // 2. Otherwise, find the Shift closest to current time (start or end).
                    // 3. Otherwise, fallback to any closest event.
                    
                    val shifts = sorted.filter { it.type == CalendarEventTypeEnum.SHIFT }
                    
                    val selected = if (shifts.isNotEmpty()) {
                        shifts.minByOrNull { shift ->
                            val start = CalendarEventHelper.parseTime(shift.startTime)
                            val end = shift.endTime?.let { CalendarEventHelper.parseTime(it) } ?: start.plusHours(1)
                            
                            val startDiff = abs(Duration.between(now, start).toMinutes())
                            val endDiff = abs(Duration.between(now, end).toMinutes())
                            
                            // Prefer events where current time is between start and end
                            if (now.isAfter(start) && now.isBefore(end)) 0L else minOf(startDiff, endDiff)
                        }
                    } else {
                        sorted.minByOrNull { event ->
                            val start = CalendarEventHelper.parseTime(event.startTime)
                            val end = event.endTime?.let { CalendarEventHelper.parseTime(it) } ?: start.plusHours(1)
                            
                            val startDiff = abs(Duration.between(now, start).toMinutes())
                            val endDiff = abs(Duration.between(now, end).toMinutes())
                            
                            if (now.isAfter(start) && now.isBefore(end)) 0L else minOf(startDiff, endDiff)
                        }
                    }
                    
                    _uiState.update { it.copy(selectedEventId = selected?.id) }
                }
            }
        }
    }

    fun loadData() {
        val options = settingsManager.getWorktimeOptions()
        val logSchedule = options.logSchedule.mapKeys { DayOfWeek.valueOf(it.key) }
        val startDay = try { DayOfWeek.valueOf(options.weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }
        
        val today = LocalDate.now().dayOfWeek
        val defaultHours = logSchedule[today]?.hours ?: 8
        val defaultMinutes = logSchedule[today]?.minutes ?: 0

        _uiState.update { it.copy(
            mode = if (options.lastMode == WorkModeEnum.QUICK_START) WorkModeEnum.QUICK_START else WorkModeEnum.CALENDAR_EVENT,
            logSchedule = logSchedule,
            weeklyStartDay = startDay,
            isSessionActive = options.isSessionActive,
            activeSessionMode = options.activeSessionMode,
            activeEventId = options.activeEventId,
            
            freehandStart = options.freehandStart?.let { s -> LocalDateTime.parse(s) } ?: LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
            freehandEnd = options.freehandEnd?.let { s -> LocalDateTime.parse(s) },
            freehandDurationHours = options.freehandDurationHours.takeIf { it > 0 } ?: defaultHours,
            freehandDurationMinutes = options.freehandDurationMinutes.takeIf { it > 0 } ?: defaultMinutes,
            isUsingDuration = options.freehandUseDuration,
            
            todayRequiredHours = defaultHours,
            todayRequiredMinutes = defaultMinutes
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
        val now = LocalDateTime.now()

        // 1. Update Event Timer (Informational only)
        val eventTimer = if (state.selectedEventId != null) {
            val event = state.todayEvents.find { it.id == state.selectedEventId }
            if (event != null) {
                val today = LocalDate.now()
                val eventStart = LocalDateTime.of(today, CalendarEventHelper.parseTime(event.startTime))
                val eventEnd = event.endTime?.let { 
                    val t = CalendarEventHelper.parseTime(it)
                    if (t.isBefore(eventStart.toLocalTime())) LocalDateTime.of(today.plusDays(1), t)
                    else LocalDateTime.of(today, t)
                } ?: eventStart.plusHours(1)

                if (now.isBefore(eventStart)) {
                    val remaining = Duration.between(now, eventStart).toMillis()
                    TimerState(
                        timeLeftMillis = remaining,
                        progress = 0f,
                        label = Loc.Workday.untilStart
                    )
                } else {
                    // Ongoing or Overtime
                    val total = Duration.between(eventStart, eventEnd).toMillis()
                    val elapsed = Duration.between(eventStart, now).toMillis()
                    val remaining = total - elapsed
                    
                    if (remaining > 0) {
                        TimerState(
                            timeLeftMillis = remaining,
                            progress = (elapsed.toFloat() / total).coerceIn(0f, 1f),
                            label = "${Loc.Workday.finishAt} ${event.endTime}"
                        )
                    } else {
                        // Overtime
                        TimerState(
                            overtimeMillis = -remaining,
                            progress = 1f,
                            isFinished = true,
                            label = Loc.Workday.overtime
                        )
                    }
                }
            } else TimerState()
        } else TimerState()

        // 2. Update Quick Start Timer (Persistent session)
        val quickStartTimer = if (state.isSessionActive && state.activeSessionMode == WorkModeEnum.QUICK_START) {
            val start = state.freehandStart ?: now
            val targetEnd = start.plusHours(state.freehandDurationHours.toLong()).plusMinutes(state.freehandDurationMinutes.toLong())
            
            if (Duration.between(start, now).toHours() >= 24) {
                viewModelScope.launch { onEvent(WorkdayTrackerEvent.FinishSession) }
                TimerState()
            } else {
                val total = Duration.between(start, targetEnd).toMillis()
                val elapsed = Duration.between(start, now).toMillis()
                val remaining = total - elapsed
                
                TimerState(
                    timeLeftMillis = if (remaining > 0) remaining else 0L,
                    overtimeMillis = if (remaining < 0) -remaining else 0L,
                    progress = if (total > 0) (elapsed.toFloat() / total).coerceIn(0f, 1f) else 0f,
                    isFinished = remaining <= 0,
                    label = if (remaining <= 0) Loc.Workday.overtime else Loc.Workday.timeLeft
                )
            }
        } else TimerState()

        _uiState.update { it.copy(
            eventTimer = eventTimer,
            quickStartTimer = quickStartTimer,
            timeLeftMillis = if (state.mode == WorkModeEnum.QUICK_START) quickStartTimer.timeLeftMillis else eventTimer.timeLeftMillis,
            overtimeMillis = if (state.mode == WorkModeEnum.QUICK_START) quickStartTimer.overtimeMillis else eventTimer.overtimeMillis,
            progress = if (state.mode == WorkModeEnum.QUICK_START) quickStartTimer.progress else eventTimer.progress,
            isFinished = if (state.mode == WorkModeEnum.QUICK_START) quickStartTimer.isFinished else eventTimer.isFinished,
            countdownLabel = if (state.mode == WorkModeEnum.QUICK_START) quickStartTimer.label else eventTimer.label
        ) }
    }

    fun onEvent(event: WorkdayTrackerEvent) {
        when (event) {
            is WorkdayTrackerEvent.ChangeMode -> {
                _uiState.update { it.copy(mode = event.mode) }
                saveOptions()
            }
            is WorkdayTrackerEvent.SetFreehandStart -> {
                _uiState.update { it.copy(freehandStart = event.dateTime, showDatePicker = false, showTimePicker = false) }
            }
            is WorkdayTrackerEvent.SetFreehandEnd -> {
                _uiState.update { it.copy(freehandEnd = event.dateTime, showDatePicker = false, showTimePicker = false) }
            }
            is WorkdayTrackerEvent.SetFreehandDuration -> {
                _uiState.update { it.copy(freehandDurationHours = event.hours, freehandDurationMinutes = event.minutes) }
            }
            is WorkdayTrackerEvent.SetUseDuration -> {
                _uiState.update { it.copy(isUsingDuration = event.use) }
            }
            is WorkdayTrackerEvent.SetWeeklyTodayStart -> {
                _uiState.update { it.copy(weeklyTodayStart = event.time, showTimePicker = false) }
                saveOptions()
            }
            is WorkdayTrackerEvent.SetWeeklyTodayEnd -> {
                _uiState.update { it.copy(weeklyTodayEnd = event.time, showTimePicker = false) }
                saveOptions()
            }
            is WorkdayTrackerEvent.ToggleWeeklyAdjustment -> {
                _uiState.update { it.copy(isWeeklyAdjustmentActive = event.active) }
                saveOptions()
            }
            WorkdayTrackerEvent.StartSession -> {
                if (_uiState.value.isSessionActive) {
                    _uiState.update { it.copy(showStartConfirm = true) }
                } else {
                    onEvent(WorkdayTrackerEvent.ConfirmStartSession)
                }
            }
            WorkdayTrackerEvent.ConfirmStartSession -> {
                val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                val currentMode = _uiState.value.mode
                
                if (currentMode == WorkModeEnum.QUICK_START) {
                    val start = _uiState.value.freehandStart ?: now
                    _uiState.update { it.copy(freehandStart = start, isSessionActive = true, activeSessionMode = currentMode, showStartConfirm = false) }
                    
                    viewModelScope.launch {
                        val userId = settingsManager.getUserId()
                        val newEvent = CalendarEvent(
                            id = UUID.randomUUID().toString(),
                            title = Loc.Calendar.shift,
                            type = CalendarEventTypeEnum.SHIFT,
                            date = start.toLocalDate().toString(),
                            startTime = start.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString(),
                            endTime = null,
                            createdBy = userId,
                            ownerUid = userId,
                            status = CalendarEventStatusEnum.REGULAR,
                            dtStart = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            dtEnd = start.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                        
                        var eventToSave = newEvent
                        val calendarId = settingsManager.getGoogleCalendarId()
                        val syncEnabled = settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPushEnabled()
                        
                        if (syncEnabled && calendarId.isNotEmpty() && calendarId != "primary") {
                            try {
                                val gId = GoogleCalendarSyncHelper.pushToGoogleCalendar(
                                    dataManager.context, 
                                    eventToSave, 
                                    calendarId
                                )
                                if (gId != null) {
                                    eventToSave = eventToSave.copy(googleEventId = gId.toString(), googleCalendarId = calendarId)
                                }
                            } catch (_: Exception) {}
                        }

                        // NEW STRATEGY: If successfully synced to Google, don't save to Firestore (Quick Start is always private)
                        if (eventToSave.googleEventId != null) {
                            // Already in Google, delete any accidental local copy
                            dataManager.deleteCalendarEventLocalOnly(eventToSave.id)
                        } else {
                            dataManager.saveCalendarEvent(eventToSave)
                        }
                        
                        _uiState.update { it.copy(activeEventId = eventToSave.id) }
                        saveOptions()
                    }
                } else {
                    _uiState.update { it.copy(showStartConfirm = false) }
                }
            }
            WorkdayTrackerEvent.ResetSession -> {
                val state = _uiState.value
                val activeId = state.activeEventId
                
                clearSessionState()

                if (activeId != null) {
                    viewModelScope.launch {
                        dataManager.deleteCalendarEvent(activeId)
                    }
                }
            }
            WorkdayTrackerEvent.FinishSession -> {
                val state = _uiState.value
                val isQuickStart = state.mode == WorkModeEnum.QUICK_START
                val isCalendarEvent = state.mode == WorkModeEnum.CALENDAR_EVENT
                
                if (state.isSessionActive || (isCalendarEvent && state.eventTimer.label?.contains(Loc.Workday.finishAt) == true)) {
                    viewModelScope.launch {
                        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                        val userId = settingsManager.getUserId()
                        val eventId = if (isQuickStart) state.activeEventId else state.selectedEventId
                        
                        if (eventId != null) {
                            val todayMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val tomorrowMs = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val events = dataManager.allCalendarEventsFlow(todayMs, tomorrowMs).first()
                            
                            val existing = events.find { it.id == eventId }

                            if (existing != null) {
                                val updated = existing.copy(
                                    endTime = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString(),
                                    status = CalendarEventStatusEnum.FINISHED_WORKDAY,
                                    dtEnd = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                )
                                
                                if (updated.googleEventId != null && updated.googleCalendarId != null) {
                                    try {
                                        GoogleCalendarSyncHelper.pushToGoogleCalendar(
                                            dataManager.context,
                                            updated,
                                            updated.googleCalendarId
                                        )
                                    } catch (_: Exception) {}
                                }

                                // If it's private and synced, it shouldn't be in Firestore
                                if (updated.sharedGroupIds.isEmpty() && updated.googleEventId != null) {
                                    dataManager.deleteCalendarEventLocalOnly(updated.id)
                                } else {
                                    dataManager.saveCalendarEvent(updated)
                                }
                                
                                val workItem = WorkHistoryItem(
                                    date = updated.date,
                                    startTime = updated.startTime,
                                    endTime = updated.endTime ?: updated.startTime,
                                    mode = "TRACKER"
                                )
                                dataManager.addHistoryItem(userId, workItem)
                            }
                        }
                        if (isQuickStart) clearSessionState()
                        else loadData() // Refresh informational state
                    }
                } else {
                    if (isQuickStart) clearSessionState()
                }
            }
            is WorkdayTrackerEvent.TogglePicker -> {
                _uiState.update { it.copy(
                    showDatePicker = (event.picker == PickerTypeEnum.DATE && event.show),
                    showTimePicker = (event.picker == PickerTypeEnum.TIME && event.show),
                    pickingStartTime = event.isStart,
                    isPickingWeeklyToday = event.forWeeklyToday
                ) }
            }
            is WorkdayTrackerEvent.ToggleResetConfirm -> _uiState.update { it.copy(showResetConfirm = event.show) }
            is WorkdayTrackerEvent.ToggleStartConfirm -> _uiState.update { it.copy(showStartConfirm = event.show) }
            is WorkdayTrackerEvent.ToggleQuickStartError -> _uiState.update { it.copy(showQuickStartConfigError = event.show) }
            WorkdayTrackerEvent.Refresh -> {
                viewModelScope.launch {
                    try {
                        dataManager.fetchUserSettings()
                    } catch (_: Exception) {}
                    loadData()
                }
            }
            is WorkdayTrackerEvent.SelectEvent -> {
                settingsManager.setSelectedCalendarEventId(event.eventId)
                val sel = _uiState.value.todayEvents.find { it.id == event.eventId }
                if (sel != null) settingsManager.setSelectedCalendarDate(sel.date)
                _uiState.update { it.copy(selectedEventId = event.eventId, isCountingDown = false) }
            }
            WorkdayTrackerEvent.StartNow -> {
                _uiState.update { it.copy(isCountingDown = false) }
                onEvent(WorkdayTrackerEvent.ConfirmStartSession)
            }
            WorkdayTrackerEvent.StartCountdown -> {
                _uiState.update { it.copy(isCountingDown = true) }
            }
            else -> {}
        }
    }

    private fun clearSessionState() {
        _uiState.update { it.copy(
            isSessionActive = false,
            activeSessionMode = null,
            activeEventId = null,
            showResetConfirm = false,
            showQuickStartConfigError = false,
            quickStartTimer = TimerState()
        ) }
        saveOptions()
    }

    private fun saveOptions() {
        val current = _uiState.value
        val options = settingsManager.getWorktimeOptions().copy(
            lastMode = current.mode,
            isSessionActive = current.isSessionActive,
            activeSessionMode = current.activeSessionMode,
            activeEventId = current.activeEventId,
            freehandStart = if (current.isSessionActive) current.freehandStart?.toString() else null,
            freehandEnd = if (current.isSessionActive) current.freehandEnd?.toString() else null,
            freehandDurationHours = current.freehandDurationHours,
            freehandDurationMinutes = current.freehandDurationMinutes,
            freehandUseDuration = current.isUsingDuration
        )
        
        settingsManager.saveWorktimeOptions(options)
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.pushUserSettings()
        }
    }
}
