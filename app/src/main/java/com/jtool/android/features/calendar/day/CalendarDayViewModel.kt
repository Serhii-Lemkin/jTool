package com.jtool.android.features.calendar.day

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.workday.WorkHistoryItem
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.util.UUID

class CalendarDayViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarDayUiState())
    val uiState: StateFlow<CalendarDayUiState> = _uiState.asStateFlow()

    private val _googleEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())

    fun getWorkdayStartHour(): Int {
        val options = settingsManager.getWorktimeOptions()
        val timeStr = if (options.useStableApproximateHours) {
            options.stableApproximateStart
        } else {
            options.logSchedule[LocalDate.now().dayOfWeek.name]?.approximateStartTime
        } ?: options.scheduleStart ?: "08:00"
        
        return try {
            timeStr.split(":")[0].toInt()
        } catch (_: Exception) {
            8
        }
    }

    init {
        refresh()
    }

    fun refresh() {
        val date = LocalDate.parse(settingsManager.getSelectedCalendarDate())
        val shouldOpenAdd = settingsManager.shouldAutoOpenCalendarAdd()
        if (shouldOpenAdd) {
            settingsManager.setAutoOpenCalendarAdd(false)
        }
        _uiState.update { it.copy(selectedDate = date, showAddEditDialog = shouldOpenAdd) }
        loadEvents(date)
    }

    fun onDateSelected(date: LocalDate) {
        settingsManager.setSelectedCalendarDate(date.toString())
        _uiState.update { it.copy(selectedDate = date) }
        loadEvents(date)
    }

    fun navigateDay(direction: Int) {
        val next = _uiState.value.selectedDate.plusDays(direction.toLong())
        onDateSelected(next)
    }

    fun toggleViewMode() {
        val nextMode = if (_uiState.value.viewMode == CalendarDayViewMode.TIMELINE) CalendarDayViewMode.LIST else CalendarDayViewMode.TIMELINE
        _uiState.update { it.copy(viewMode = nextMode) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadEvents(date: LocalDate) {
        val options = settingsManager.getWorktimeOptions()
        
        val startMs = date.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 1. Fetch Google Events for this specific day (and prev for overnight)
        viewModelScope.launch {
            if (settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPullEnabled()) {
                val calendarId = settingsManager.getGoogleCalendarId()
                if (calendarId.isNotEmpty() && calendarId != "primary") {
                    val events = GoogleCalendarSyncHelper.pullFromGoogleCalendar(dataManager.context, settingsManager.getUserId(), calendarId, startMs, endMs)
                    _googleEvents.value = events ?: emptyList()
                }
            } else {
                _googleEvents.value = emptyList()
            }
        }

        // 2. Combine with Firestore
        combine(dataManager.allCalendarEventsFlow(startMs, endMs), _googleEvents) { firestoreEvents, googleEvents ->
            Log.d("CalendarDayVM", "Merging Firestore(${firestoreEvents.size}) and Google(${googleEvents.size}) events")
            
            // cleanup: delete private synced from Firestore
            val privateSyncedIds = firestoreEvents
                .filter { it.sharedGroupIds.isEmpty() && !it.googleEventId.isNullOrEmpty() }
                .map { it.id }
            
            if (privateSyncedIds.isNotEmpty()) {
                viewModelScope.launch {
                    privateSyncedIds.forEach { dataManager.deleteCalendarEventLocalOnly(it) }
                }
            }

            (firestoreEvents.filter { !privateSyncedIds.contains(it.id) } + googleEvents).distinctBy { it.googleEventId ?: it.id }
        }.onEach { allEvents ->
            val virtualEvents = if (options.trackingPreference == WorkTrackingPreferenceEnum.LOG_HOURS) {
                CalendarEventHelper.generateVirtualShifts(options, date, date)
            } else emptyList()
            
            val displayEvents = mutableListOf<CalendarEvent>()
            
            // Process events
            allEvents.forEach { event ->
                val eventStartLdt = event.dtStart?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
                    ?: LocalDate.parse(event.date).atTime(CalendarEventHelper.parseTime(event.startTime))
                
                val eventDate = eventStartLdt.toLocalDate()
                
                val eventEndLdt = event.dtEnd?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
                    ?: if (event.endTime.isNullOrBlank()) eventStartLdt.plusHours(1) else LocalDate.parse(event.date).atTime(CalendarEventHelper.parseTime(event.endTime!!))
                
                val isOvernight = eventEndLdt.toLocalDate().isAfter(eventStartLdt.toLocalDate())

                if (eventDate == date) {
                    displayEvents.add(event)
                } else if (eventDate == date.minusDays(1) && isOvernight) {
                    displayEvents.add(event.copy(
                        startTime = "00:00",
                        note = "Continued from yesterday. ${event.note}".trim(),
                        id = "${event.id}_cont"
                    ))
                }
            }

            // Merge virtual shifts if today has no real shift
            val hasRealShift = displayEvents.any { it.type == CalendarEventTypeEnum.SHIFT && !it.id.startsWith("virtual_") }
            if (!hasRealShift) {
                displayEvents.addAll(virtualEvents)
            }

            _uiState.update { it.copy(privateEvents = displayEvents.sortedBy { it.dtStart ?: 0L }) }
        }.launchIn(viewModelScope)
    }

    fun toggleAddEditDialog(show: Boolean, event: CalendarEvent? = null) {
        if (event != null) {
            settingsManager.setSelectedCalendarEventId(event.id)
            if (event.id.startsWith("virtual_")) {
                _uiState.update { it.copy(
                    showAddEditDialog = show, 
                    editingEvent = event.copy(id = "", title = Loc.Calendar.shift, note = "")
                ) }
                return
            }
        }
        _uiState.update { it.copy(showAddEditDialog = show, editingEvent = event) }
    }

    fun addEvent(context: Context, event: CalendarEvent, selectedGroupIds: List<String>) {
        viewModelScope.launch {
            val userId = settingsManager.getUserId()
            
            var eventToSave = if (event.id.isEmpty()) event.copy(id = UUID.randomUUID().toString()) else event

            if (eventToSave.dtStart == null && eventToSave.date.isNotEmpty()) {
                try {
                    val startLdt = LocalDate.parse(eventToSave.date).atTime(CalendarEventHelper.parseTime(eventToSave.startTime))
                    val endLdt = eventToSave.endTime?.let { LocalDate.parse(eventToSave.date).atTime(CalendarEventHelper.parseTime(it)) } ?: startLdt.plusHours(1)
                    eventToSave = eventToSave.copy(
                        dtStart = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        dtEnd = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                } catch (_: Exception) {}
            }

            val keywords = settingsManager.getAutoWorkKeywords().split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val matchedKeyword = keywords.find { eventToSave.title.lowercase().contains(it) }
            if (matchedKeyword != null && !eventToSave.note.contains(matchedKeyword, ignoreCase = true)) {
                val newNote = if (eventToSave.note.isEmpty()) matchedKeyword else "${eventToSave.note}\n$matchedKeyword"
                eventToSave = eventToSave.copy(note = newNote)
            }

            val gCalendarId = settingsManager.getGoogleCalendarId()
            val syncEnabled = settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPushEnabled()
            
            if (gCalendarId.isNotEmpty() && gCalendarId != "primary" && (eventToSave.googleEventId != null || syncEnabled)) {
                try {
                    val gId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToSave, gCalendarId)
                    if (gId != null) {
                        eventToSave = eventToSave.copy(googleEventId = gId.toString(), googleCalendarId = gCalendarId)
                    }
                } catch (e: Exception) {
                    Log.e("CalendarDayVM", "Google Sync failed", e)
                }
            } else if (eventToSave.googleEventId != null) {
                // UNSYNC: Delete from Google
                try {
                    GoogleCalendarSyncHelper.deleteFromGoogleCalendar(context, eventToSave.googleEventId!!)
                    eventToSave = eventToSave.copy(googleEventId = null, googleCalendarId = null)
                } catch (_: Exception) {}
            }

            // NEW STRATEGY:
            if (selectedGroupIds.isEmpty() && eventToSave.googleEventId != null) {
                dataManager.deleteCalendarEventLocalOnly(eventToSave.id)
            } else {
                dataManager.saveCalendarEvent(eventToSave.copy(ownerUid = userId, sharedGroupIds = selectedGroupIds))
            }
            
            if (eventToSave.type == CalendarEventTypeEnum.SHIFT || matchedKeyword != null) {
                val workItem = WorkHistoryItem(
                    date = eventToSave.date,
                    startTime = eventToSave.startTime,
                    endTime = eventToSave.endTime ?: eventToSave.startTime,
                    mode = "CALENDAR"
                )
                dataManager.addHistoryItem(userId, workItem)
            }
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            dataManager.deleteCalendarEvent(event.id)
        }
    }
}
