package com.jtool.android.features.workday.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.workday.DayLogHours
import com.jtool.android.core.models.workday.DaySchedule
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventStatusEnum
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

class WorkdayTrackerViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkdayTrackerUiState())
    val uiState: StateFlow<WorkdayTrackerUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        loadData()
        startTicker()
        checkTodayEvents()
    }

    private fun checkTodayEvents() {
        viewModelScope.launch {
            val userId = settingsManager.getUserId()
            val today = LocalDate.now().toString()
            dataManager.getPrivateCalendarEventsFlow(userId, today, today).collectLatest { events ->
                val activeId = settingsManager.getWorktimeOptions().activeEventId
                // Priority: active event first, then any other SHIFT
                val shift = events.find { it.id == activeId } ?: events.find { it.type == CalendarEventTypeEnum.SHIFT }
                _uiState.update { it.copy(todayCalendarShift = shift) }
            }
        }
    }

    fun loadData() {
        val options = settingsManager.getWorktimeOptions()
        
        val logSchedule = options.logSchedule.mapKeys { DayOfWeek.valueOf(it.key) }
        val startDay = try { DayOfWeek.valueOf(options.weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }
        
        _uiState.update { it.copy(
            mode = options.lastMode,
            trackingPreference = options.trackingPreference,
            logSchedule = logSchedule,
            weeklyStartDay = startDay,
            isSessionActive = options.isSessionActive,
            activeSessionMode = options.activeSessionMode,
            activeEventId = options.activeEventId,
            
            freehandStart = options.freehandStart?.let { s -> LocalDateTime.parse(s) } ?: LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
            freehandEnd = options.freehandEnd?.let { s -> LocalDateTime.parse(s) },
            freehandDurationHours = options.freehandDurationHours,
            freehandDurationMinutes = options.freehandDurationMinutes,
            isUsingDuration = options.freehandUseDuration,
            
            logHoursSessionStart = options.logHoursSessionStart?.let { s -> LocalDateTime.parse(s) },
            isLogHoursSet = options.isLogHoursSet,
            
            weeklyTodayStart = options.weeklyTodayStart?.let { s -> CalendarEventHelper.parseTime(s) },
            weeklyTodayEnd = options.weeklyTodayEnd?.let { s -> CalendarEventHelper.parseTime(s) },
            isWeeklyAdjustmentActive = options.isWeeklyAdjustmentActive,
            
            scheduleStart = options.scheduleStart?.let { s -> LocalDateTime.parse(s) },
            scheduleEnd = options.scheduleEnd?.let { s -> LocalDateTime.parse(s) }
        ) }

        // Adjust mode if not allowed by preference
        val pref = options.trackingPreference
        val currentMode = _uiState.value.mode
        if (pref == null || pref == WorkTrackingPreferenceEnum.UNSET) {
            if (currentMode != WorkModeEnum.QUICK_START) {
                onEvent(WorkdayTrackerEvent.ChangeMode(WorkModeEnum.QUICK_START))
            }
        } else if (pref == WorkTrackingPreferenceEnum.LOG_HOURS && currentMode == WorkModeEnum.SCHEDULE) {
            onEvent(WorkdayTrackerEvent.ChangeMode(WorkModeEnum.LOG_HOURS))
        } else if (pref == WorkTrackingPreferenceEnum.SCHEDULE && currentMode == WorkModeEnum.LOG_HOURS) {
            onEvent(WorkdayTrackerEvent.ChangeMode(WorkModeEnum.SCHEDULE))
        }
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
        
        if (!state.isSessionActive) {
            resetCalculations()
            return
        }

        val modeToUse = state.activeSessionMode ?: state.mode

        when (modeToUse) {
            WorkModeEnum.QUICK_START -> {
                val start = state.freehandStart ?: return
                val targetEnd = if (state.isUsingDuration) {
                    start.plusHours(state.freehandDurationHours.toLong()).plusMinutes(state.freehandDurationMinutes.toLong())
                } else {
                    state.freehandEnd ?: return
                }
                checkAutoFinish(now, start)
                updateCalculations(now, start, targetEnd)
            }
            WorkModeEnum.LOG_HOURS -> {
                val start = state.logHoursSessionStart ?: return
                val today = LocalDate.now().dayOfWeek
                val config = state.logSchedule[today] ?: DayLogHours()
                
                if (config.isConfirmedOff) {
                    onEvent(WorkdayTrackerEvent.ResetSession)
                    return
                }

                val targetEnd = start.plusHours(config.hours.toLong()).plusMinutes(config.minutes.toLong())
                checkAutoFinish(now, start)
                updateCalculations(now, start, targetEnd)
            }
            WorkModeEnum.SCHEDULE -> {
                val start = state.scheduleStart ?: return
                val end = state.scheduleEnd ?: return
                checkAutoFinish(now, start)
                updateCalculations(now, start, end)
            }
        }
    }

    private fun checkAutoFinish(now: LocalDateTime, start: LocalDateTime) {
        if (Duration.between(start, now).toHours() >= 24) {
            viewModelScope.launch {
                val event = CalendarEvent(
                    title = "Shift (${_uiState.value.activeSessionMode?.name ?: _uiState.value.mode.name})",
                    type = CalendarEventTypeEnum.SHIFT,
                    date = start.toLocalDate().toString(),
                    startTime = start.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString(),
                    endTime = null, // Auto-finished
                    createdBy = settingsManager.getUserId(),
                    status = CalendarEventStatusEnum.UNFINISHED_WORKDAY
                )
                dataManager.addPrivateCalendarEvent(settingsManager.getUserId(), event)
                onEvent(WorkdayTrackerEvent.ResetSession)
            }
        }
    }

    private fun resetCalculations() {
        _uiState.update { it.copy(
            timeLeftMillis = 0L,
            overtimeMillis = 0L,
            progress = 0f,
            isFinished = false
        ) }
    }

    private fun updateCalculations(now: LocalDateTime, start: LocalDateTime, end: LocalDateTime) {
        val totalDuration = Duration.between(start, end).toMillis()
        val elapsed = Duration.between(start, now).toMillis()
        val remaining = totalDuration - elapsed
        
        val progress = if (totalDuration > 0) (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f
        
        _uiState.update { it.copy(
            timeLeftMillis = if (remaining > 0) remaining else 0L,
            overtimeMillis = if (remaining < 0) -remaining else 0L,
            progress = progress,
            isFinished = remaining <= 0
        ) }
    }

    fun onEvent(event: WorkdayTrackerEvent) {
        when (event) {
            is WorkdayTrackerEvent.ChangeMode -> {
                _uiState.update { it.copy(mode = event.mode) }
                saveOptions()
            }
            is WorkdayTrackerEvent.SetTrackingPreference -> {
                _uiState.update { it.copy(trackingPreference = event.preference) }
                val targetMode = if (event.preference == WorkTrackingPreferenceEnum.LOG_HOURS) WorkModeEnum.LOG_HOURS else WorkModeEnum.SCHEDULE
                onEvent(WorkdayTrackerEvent.ChangeMode(targetMode))
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
                if (_uiState.value.mode == WorkModeEnum.QUICK_START) {
                    val state = _uiState.value
                    val isValid = if (state.isUsingDuration) {
                        state.freehandDurationHours > 0 || state.freehandDurationMinutes > 0
                    } else {
                        state.freehandEnd != null
                    }
                    if (!isValid) {
                        _uiState.update { it.copy(showQuickStartConfigError = true) }
                        return
                    }
                }
                
                if (_uiState.value.isSessionActive) {
                    _uiState.update { it.copy(showStartConfirm = true) }
                } else {
                    onEvent(WorkdayTrackerEvent.ConfirmStartSession)
                }
            }
            WorkdayTrackerEvent.ConfirmStartSession -> {
                val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                val currentMode = _uiState.value.mode
                var eventId: String? = null
                
                when (currentMode) {
                    WorkModeEnum.QUICK_START -> {
                        val start = _uiState.value.freehandStart ?: now
                        _uiState.update { it.copy(freehandStart = start, isSessionActive = true, activeSessionMode = currentMode, showStartConfirm = false) }
                    }
                    WorkModeEnum.SCHEDULE -> {
                        val shift = _uiState.value.todayCalendarShift
                        if (shift != null) {
                            val today = LocalDate.now()
                            val startTime = CalendarEventHelper.parseTime(shift.startTime)
                            val endTime = shift.endTime?.let { CalendarEventHelper.parseTime(it) }
                            val start = LocalDateTime.of(today, startTime)
                            var end = endTime?.let { LocalDateTime.of(today, it) }
                            if (end != null && end.isBefore(start)) end = end.plusDays(1)
                            
                            _uiState.update { it.copy(scheduleStart = start, scheduleEnd = end, isSessionActive = true, activeSessionMode = currentMode, showStartConfirm = false) }
                            eventId = shift.id
                        }
                    }
                    WorkModeEnum.LOG_HOURS -> {
                        _uiState.update { it.copy(logHoursSessionStart = now, isSessionActive = true, activeSessionMode = currentMode, showStartConfirm = false) }
                    }
                }

                // For QUICK_START and LOG_HOURS, create a pending SHIFT event in DB
                if (currentMode != WorkModeEnum.SCHEDULE) {
                    val start = when (currentMode) {
                        WorkModeEnum.QUICK_START -> _uiState.value.freehandStart ?: now
                        WorkModeEnum.LOG_HOURS -> now
                        else -> now
                    }
                    
                    viewModelScope.launch {
                        val userId = settingsManager.getUserId()
                        val newEvent = CalendarEvent(
                            title = "Shift (${currentMode.name})",
                            type = CalendarEventTypeEnum.SHIFT,
                            date = start.toLocalDate().toString(),
                            startTime = start.toLocalTime().toString(),
                            endTime = null, // In progress
                            createdBy = userId,
                            status = CalendarEventStatusEnum.REGULAR
                        )
                        val docId = java.util.UUID.randomUUID().toString()
                        val eventWithId = newEvent.copy(id = docId)
                        dataManager.addPrivateCalendarEvent(userId, eventWithId)
                        
                        _uiState.update { it.copy(activeEventId = docId) }
                        saveOptions()
                    }
                } else {
                    _uiState.update { it.copy(activeEventId = eventId) }
                    saveOptions()
                }
            }
            WorkdayTrackerEvent.ResetSession -> {
                val state = _uiState.value
                val activeId = state.activeEventId
                val userId = settingsManager.getUserId()
                val sessionMode = state.activeSessionMode ?: state.mode
                
                clearSessionState()

                // Only delete if it was an auto-created temporary shift (Quick Start or Log Hours)
                if (activeId != null && sessionMode != WorkModeEnum.SCHEDULE) {
                    viewModelScope.launch {
                        dataManager.deletePrivateCalendarEvent(userId, activeId)
                    }
                }
            }
            WorkdayTrackerEvent.FinishSession -> {
                val state = _uiState.value
                if (state.isSessionActive) {
                    viewModelScope.launch {
                        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                        val userId = settingsManager.getUserId()
                        val eventId = state.activeEventId
                        
                        if (eventId != null) {
                            // Find existing event to update it with end time
                            val today = LocalDate.now().toString()
                            dataManager.getPrivateCalendarEventsFlow(userId, today, today)
                                .take(1)
                                .collect { events ->
                                    val existing = events.find { it.id == eventId }
                                    if (existing != null) {
                                        val updated = existing.copy(
                                            endTime = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString(),
                                            status = CalendarEventStatusEnum.FINISHED_WORKDAY
                                        )
                                        dataManager.addPrivateCalendarEvent(userId, updated)
                                    }
                                }
                        }
                        clearSessionState()
                    }
                } else {
                    clearSessionState()
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
        }
    }

    private fun clearSessionState() {
        _uiState.update { it.copy(
            scheduleStart = null,
            scheduleEnd = null,
            weeklyTodayStart = null,
            weeklyTodayEnd = null,
            isWeeklyAdjustmentActive = false,
            showResetConfirm = false,
            showQuickStartConfigError = false,
            isSessionActive = false,
            activeSessionMode = null,
            activeEventId = null,
            isFinished = false,
            timeLeftMillis = 0L,
            overtimeMillis = 0L,
            progress = 0f
        ) }
        saveOptions()
    }

    private fun saveOptions() {
        val current = _uiState.value
        val options = settingsManager.getWorktimeOptions().copy(
            lastMode = current.mode,
            trackingPreference = current.trackingPreference,
            isSessionActive = current.isSessionActive,
            activeSessionMode = current.activeSessionMode,
            activeEventId = current.activeEventId,
            isWeeklyAdjustmentActive = current.isWeeklyAdjustmentActive,
            weeklyTodayStart = current.weeklyTodayStart?.toString(),
            weeklyTodayEnd = current.weeklyTodayEnd?.toString(),
            scheduleStart = current.scheduleStart?.toString(),
            scheduleEnd = current.scheduleEnd?.toString(),
            freehandStart = if (current.isSessionActive) current.freehandStart?.toString() else null,
            freehandEnd = if (current.isSessionActive) current.freehandEnd?.toString() else null,
            freehandDurationHours = current.freehandDurationHours,
            freehandDurationMinutes = current.freehandDurationMinutes,
            freehandUseDuration = current.isUsingDuration,
            logHoursSessionStart = if (current.isSessionActive) current.logHoursSessionStart?.toString() else null
        )
        
        settingsManager.saveWorktimeOptions(options)
        viewModelScope.launch {
            dataManager.pushUserSettings()
        }
    }
}
