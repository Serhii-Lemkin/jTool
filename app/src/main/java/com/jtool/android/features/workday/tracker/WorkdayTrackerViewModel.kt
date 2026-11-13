package com.jtool.android.features.workday.tracker

import android.provider.CalendarContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
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
            pullTodayGoogleEventsInternal()
        }
    }

    private suspend fun pullTodayGoogleEventsInternal(): List<CalendarEvent> {
        val calendarId = settingsManager.getGoogleCalendarId()
        val displayedIds = settingsManager.getDisplayedCalendarIds()
        val effectiveIds = if (displayedIds.isNotEmpty()) {
            if (calendarId.isNotEmpty()) displayedIds + calendarId else displayedIds
        } else if (calendarId.isNotEmpty()) {
            setOf(calendarId)
        } else {
            emptySet()
        }

        if (effectiveIds.isEmpty()) {
            _googleEvents.value = emptyList()
            return emptyList()
        }

        val today = LocalDate.now()
        val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val userEmail = settingsManager.getGoogleCalendarEmail()

        val allEvents = GoogleCalendarSyncHelper.pullFromMultipleGoogleCalendars(
            dataManager.context,
            effectiveIds,
            startMs,
            endMs,
            userEmail
        ).distinctBy { it.iCalUID ?: it.id }

        val userId = settingsManager.getUserId()
        val groupIds = settingsManager.getGroups().keys.toList()
        val extensions = dataManager.getAllCalendarExtensions(userId, groupIds)

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

        val sorted = (processedEvents + extraEvents).sortedBy { it.dtStart ?: 0L }
        _googleEvents.value = sorted
        return sorted
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
                    val currentEvent = allTodayEvents.find { event ->
                        val start = event.dtStart ?: 0L
                        val end = event.dtEnd ?: Long.MAX_VALUE
                        start <= now && end >= now
                    }
                    val nextEvent = allTodayEvents.filter { (it.dtStart ?: 0L) > now }.minByOrNull { it.dtStart ?: 0L }
                    val fallbackEvent = allTodayEvents.minByOrNull { event ->
                        val start = event.dtStart ?: 0L
                        Math.abs(start - now)
                    }

                    val chosenEvent = currentEvent ?: nextEvent ?: fallbackEvent
                    val isCalendarMode = _uiState.value.mode == WorkModeEnum.CALENDAR_EVENT
                    _uiState.update { it.copy(
                        todayEvents = allTodayEvents,
                        selectedEventId = chosenEvent?.id,
                        isSessionActive = if (isCalendarMode) chosenEvent != null else _uiState.value.isSessionActive
                    ) }
                } else {
                    val isCalendarMode = _uiState.value.mode == WorkModeEnum.CALENDAR_EVENT
                    _uiState.update { it.copy(
                        todayEvents = allTodayEvents,
                        selectedEventId = validSelectedId,
                        isSessionActive = if (isCalendarMode) validSelectedId != null else _uiState.value.isSessionActive
                    ) }
                }
            }
        }
    }

    private fun loadData() {
        val options = settingsManager.getWorktimeOptions()
        val now = LocalDateTime.now()
        val today = LocalDate.now().dayOfWeek
        val dayLogHours = options.logSchedule[today.name]
        val hours: Int
        val minutes: Int
        if (dayLogHours != null) {
            hours = dayLogHours.hours
            minutes = dayLogHours.minutes
        } else {
            val allHours = settingsManager.getAllWeeklyHours()
            val doubleHours = allHours[today] ?: 8.5
            hours = doubleHours.toInt()
            minutes = ((doubleHours - hours) * 60).toInt()
        }

        _uiState.update { it.copy(
            trackingPreference = options.trackingPreference,
            logSchedule = options.logSchedule.mapKeys { DayOfWeek.valueOf(it.key) },
            freehandStart = now,
            freehandEnd = now.plusHours(hours.toLong()).plusMinutes(minutes.toLong()),
            freehandDurationHours = hours,
            freehandDurationMinutes = minutes
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
        val now = System.currentTimeMillis()
        
        if (state.mode == WorkModeEnum.QUICK_START) {
            val start = if (state.quickStartStartMs > 0L) state.quickStartStartMs else (state.freehandStart?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: now)
            val end = if (state.quickStartEndMs > 0L) state.quickStartEndMs else (start + (state.freehandDurationHours * 3600L + state.freehandDurationMinutes * 60L) * 1000L)
            
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
                        isFinished -> Loc.Workday.eventEnded
                        isNotStarted -> Loc.Workday.untilStart
                        else -> Loc.Workday.finishAt
                    }
                )
            ) }
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
                            isFinished -> Loc.Workday.eventEnded
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
                _uiState.update { it.copy(
                    mode = event.mode,
                    isSessionActive = if (event.mode == WorkModeEnum.QUICK_START) false else (_uiState.value.selectedEventId != null)
                ) }
            }
            is WorkdayTrackerEvent.SelectEvent -> {
                _uiState.update { it.copy(selectedEventId = event.eventId, isSessionActive = event.eventId != null) }
            }
            is WorkdayTrackerEvent.StartSession -> {
                if (_uiState.value.mode == WorkModeEnum.QUICK_START) {
                    processQuickStart()
                } else {
                    _uiState.update { it.copy(isSessionActive = true) }
                    calculateRealTimeData()
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
                .ifEmpty { settingsManager.getDisplayedCalendarIds().firstOrNull() ?: "" }
            if (calendarId.isEmpty()) {
                _uiState.update { it.copy(showQuickStartConfigError = true, isLoading = false) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val startMs = state.freehandStart?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()

            val options = settingsManager.getWorktimeOptions()
            val today = LocalDate.now().dayOfWeek
            val dayLogHours = options.logSchedule[today.name]
            val defaultHours: Int
            val defaultMinutes: Int
            if (dayLogHours != null) {
                defaultHours = dayLogHours.hours
                defaultMinutes = dayLogHours.minutes
            } else {
                val allHours = settingsManager.getAllWeeklyHours()
                val doubleHours = allHours[today] ?: 8.5
                defaultHours = doubleHours.toInt()
                defaultMinutes = ((doubleHours - defaultHours) * 60).toInt()
            }
            val defaultDurationMs = (defaultHours * 3600L + defaultMinutes * 60L) * 1000L

            val durationMs = if (state.isUsingDuration) {
                (state.freehandDurationHours * 3600L + state.freehandDurationMinutes * 60L) * 1000L
            } else {
                val endMsVal = state.freehandEnd?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    ?: (startMs + defaultDurationMs)
                (endMsVal - startMs).coerceAtLeast(0L)
            }

            val finalDurationMs = if (durationMs <= 0L) defaultDurationMs else durationMs
            val plannedEndMs = startMs + finalDurationMs

            val workShiftColorKey = settingsManager.getWorkShiftColorKey()
            val shiftTitle = settingsManager.getCustomWorkShiftTitle()
            val newEvent = CalendarEvent(
                title = shiftTitle,
                dtStart = startMs,
                dtEnd = plannedEndMs,
                note = "Quick Start Shift",
                colorKey = workShiftColorKey,
                googleCalendarId = calendarId
            )

            val createdId = CalendarEventHelper.pushAndShareShiftOrEvent(
                context = dataManager.context,
                settingsManager = settingsManager,
                dataManager = dataManager,
                event = newEvent,
                calendarId = calendarId,
                sharedGroupIds = emptyList()
            )
            val refreshed = pullTodayGoogleEventsInternal()
            val matchedEvent = refreshed.find {
                (createdId != null && (it.id == createdId || it.iCalUID == createdId)) ||
                    (it.title == shiftTitle && Math.abs((it.dtStart ?: 0L) - startMs) <= 120_000L)
            }

            val targetEvent = matchedEvent ?: newEvent.copy(id = createdId ?: "local_$startMs")

            _uiState.update { it.copy(
                mode = WorkModeEnum.CALENDAR_EVENT,
                selectedEventId = targetEvent.id,
                isSessionActive = true,
                quickStartEventId = targetEvent.id,
                quickStartStartMs = startMs,
                quickStartEndMs = plannedEndMs,
                isLoading = false
            ) }

            if (matchedEvent == null) {
                val updatedList = (refreshed + targetEvent).sortedBy { it.dtStart ?: 0L }
                _googleEvents.value = updatedList
            } else {
                _uiState.update { it.copy(todayEvents = refreshed) }
            }
            calculateRealTimeData()
        }
    }

    private fun processFinish() {
        val state = _uiState.value
        if (!state.isSessionActive) return

        val targetEventId = state.selectedEventId ?: state.quickStartEventId
        _uiState.update { it.copy(isSessionActive = false, selectedEventId = null, quickStartEventId = null, quickStartStartMs = 0L, quickStartEndMs = 0L) }

        viewModelScope.launch {
            val nowMs = System.currentTimeMillis()
            val calendarId = settingsManager.getGoogleCalendarId()
                .ifEmpty { settingsManager.getDisplayedCalendarIds().firstOrNull() ?: "" }

            if (calendarId.isNotEmpty() && targetEventId != null) {
                val existing = _googleEvents.value.find { it.id == targetEventId || it.iCalUID == targetEventId }

                if (existing != null) {
                    val updated = existing.copy(
                        dtEnd = nowMs,
                        note = "${existing.note}\nFinished: ${LocalTime.now()}".trim()
                    )
                    val updatedId = GoogleCalendarSyncHelper.pushToGoogleCalendar(
                        dataManager.context,
                        updated,
                        calendarId,
                        settingsManager.getGoogleCalendarEmail()
                    )
                    val ext = dataManager.getCalendarExtension(updated.iCalUID ?: updated.id)
                        ?: updatedId?.let { dataManager.getCalendarExtension(it) }
                    if (ext != null) {
                        dataManager.saveCalendarExtension(ext.copy(endTimestamp = nowMs))
                    }
                } else if (state.quickStartStartMs > 0L) {
                    val updated = CalendarEvent(
                        id = targetEventId,
                        title = settingsManager.getCustomWorkShiftTitle(),
                        dtStart = state.quickStartStartMs,
                        dtEnd = nowMs,
                        note = "Quick Start Shift"
                    )
                    val updatedId = GoogleCalendarSyncHelper.pushToGoogleCalendar(
                        dataManager.context,
                        updated,
                        calendarId,
                        settingsManager.getGoogleCalendarEmail()
                    )
                    val ext = dataManager.getCalendarExtension(targetEventId)
                        ?: updatedId?.let { dataManager.getCalendarExtension(it) }
                    if (ext != null) {
                        dataManager.saveCalendarExtension(ext.copy(endTimestamp = nowMs))
                    }
                }
            }

            pullTodayGoogleEventsInternal()
        }
    }
}
