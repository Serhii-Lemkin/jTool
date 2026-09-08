package com.jtool.android.features.calendar.grid

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.core.models.calendar.DayEventSummary
import com.jtool.android.core.models.workday.WorkHistoryItem
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class CalendarGridViewModel(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarGridUiState(
        visibleGroupIds = settingsManager.getVisibleGroupIds()
    ))
    val uiState: StateFlow<CalendarGridUiState> = _uiState.asStateFlow()

    private val _googleEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())

    private var eventsJob: Job? = null

    init {
        loadEvents()
        // No automatic background sync on init to avoid race conditions and accidental deletion
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadEvents() {
        eventsJob?.cancel()
        
        val currentMonthFlow = _uiState.map { it.selectedMonth }.distinctUntilChanged()
        val visibleGroupsFlow = _uiState.map { it.visibleGroupIds }.distinctUntilChanged()

        // 1. Observe Google Calendar separately
        viewModelScope.launch {
            currentMonthFlow.collectLatest { month ->
                if (settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPullEnabled()) {
                    Log.d("CalendarVM", "Pulling Google Events for ${month}")
                    val start = month.atDay(1).minusWeeks(1)
                    val end = month.atEndOfMonth().plusWeeks(1)
                    val startMs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMs = end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    
                    val calendarId = settingsManager.getGoogleCalendarId()
                    if (calendarId.isNotEmpty() && calendarId != "primary") {
                        val events = GoogleCalendarSyncHelper.pullFromGoogleCalendar(context, settingsManager.getUserId(), calendarId, startMs, endMs)
                        Log.d("CalendarVM", "Google Pull returned ${events?.size ?: "null"} events")
                        _googleEvents.value = events ?: emptyList()
                    }
                } else {
                    _googleEvents.value = emptyList()
                }
            }
        }

        // 2. Combine Firestore and Google Events
        eventsJob = combine(currentMonthFlow, visibleGroupsFlow, _googleEvents) { month, visibleGroups, googleEvents ->
            val start = month.atDay(1).minusWeeks(1)
            val end = month.atEndOfMonth().plusWeeks(1)
            val startMs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            Log.d("CalendarVM", "Observing Firestore events range: $start to $end")
            dataManager.allCalendarEventsFlow(startMs, endMs).map { firestoreEvents ->
                Log.d("CalendarVM", "Firestore emitted ${firestoreEvents.size} events")
                
                // --- NEW STRATEGY: LOCAL CLEANUP ---
                // If a private event is already in Google, we don't need it in Firestore.
                val privateSyncedIds = firestoreEvents
                    .filter { it.sharedGroupIds.isEmpty() && !it.googleEventId.isNullOrEmpty() }
                    .map { it.id }
                
                if (privateSyncedIds.isNotEmpty()) {
                    viewModelScope.launch {
                        privateSyncedIds.forEach { dataManager.deleteCalendarEventLocalOnly(it) }
                    }
                }

                // Filter firestore events by group visibility
                val filteredFirestore = if (visibleGroups.isEmpty()) {
                    firestoreEvents.filter { !privateSyncedIds.contains(it.id) }
                } else {
                    firestoreEvents.filter { event ->
                        !privateSyncedIds.contains(event.id) &&
                        (event.sharedGroupIds.isEmpty() || event.sharedGroupIds.any { visibleGroups.contains(it) })
                    }
                }

                // Merge with Google Events
                val merged = (filteredFirestore + googleEvents).distinctBy { 
                    it.googleEventId ?: it.id 
                }

                val options = settingsManager.getWorktimeOptions()
                val virtualEvents = if (options.trackingPreference == WorkTrackingPreferenceEnum.LOG_HOURS) {
                    CalendarEventHelper.generateVirtualShifts(options, start, end)
                } else emptyList()
                
                val allEvents = merged.toMutableList()
                val realShiftDates = merged.filter { it.type == CalendarEventTypeEnum.SHIFT && !it.id.startsWith("virtual_") }.map { it.date }.toSet()
                
                virtualEvents.forEach { virtual ->
                    if (!realShiftDates.contains(virtual.date)) {
                        allEvents.add(virtual)
                    }
                }
                Log.d("CalendarVM", "Total display events: ${allEvents.size}")
                allEvents
            }
        }.flatMapLatest { it }
        .onEach { mergedEvents ->
            val summaries = mergedEvents.groupBy { it.date }.mapValues { (_, dayEvents) ->
                DayEventSummary(
                    typeCounts = dayEvents.groupBy { it.type }.mapValues { it.value.size }
                )
            }
            
            // CRITICAL FIX: Ensure editingEvent is updated if it was a Google-only event 
            // that we just re-fetched or if we navigated directly to Edit screen.
            val selectedId = settingsManager.getSelectedCalendarEventId()
            val currentEditing = _uiState.value.editingEvent
            
            val updatedEditing = if (selectedId != null && (currentEditing == null || currentEditing.id != selectedId)) {
                mergedEvents.find { it.id == selectedId || it.googleEventId == selectedId }
            } else currentEditing

            _uiState.update { it.copy(
                privateEvents = mergedEvents.sortedBy { it.dtStart ?: 0L }, 
                eventSummaries = summaries,
                editingEvent = updatedEditing,
                isLoading = false
            ) }
        }.launchIn(viewModelScope)
    }

    fun onDateSelected(date: LocalDate) {
        settingsManager.setSelectedCalendarDate(date.toString())
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onEventClicked(event: CalendarEvent) {
        settingsManager.setSelectedCalendarDate(event.date)
        settingsManager.setSelectedCalendarEventId(event.id)
        if (event.id.startsWith("virtual_")) {
            _uiState.update { it.copy(
                editingEvent = event.copy(
                    id = "", 
                    title = Loc.Calendar.shift,
                    note = ""
                ) 
            ) }
        } else {
            _uiState.update { it.copy(editingEvent = event) }
        }
    }

    fun onMonthChange(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month, isLoading = true) }
    }

    fun toggleAddEditDialog(show: Boolean, event: CalendarEvent? = null) {
        _uiState.update { it.copy(showAddEditDialog = show, editingEvent = event) }
    }

    fun toggleGroupVisibility(groupId: String) {
        val current = _uiState.value.visibleGroupIds.toMutableSet()
        if (current.contains(groupId)) current.remove(groupId) else current.add(groupId)
        _uiState.update { it.copy(visibleGroupIds = current) }
        settingsManager.saveVisibleGroupIds(current)
    }

    fun addEvent(context: Context, event: CalendarEvent, selectedGroupIds: List<String>) {
        viewModelScope.launch {
            val userId = settingsManager.getUserId()
            val groups = settingsManager.getGroups()
            
            val eventIdToUse = if (event.id.isEmpty()) UUID.randomUUID().toString() else event.id
            var eventToSave = event.copy(id = eventIdToUse, ownerUid = userId, sharedGroupIds = selectedGroupIds)

            if (eventToSave.dtStart == null && eventToSave.date.isNotEmpty()) {
                try {
                    val startLdt = LocalDate.parse(eventToSave.date).atTime(LocalTime.parse(eventToSave.startTime))
                    val endLdt = eventToSave.endTime?.let { LocalDate.parse(eventToSave.date).atTime(
                        LocalTime.parse(it)) } ?: startLdt.plusHours(1)
                    eventToSave = eventToSave.copy(
                        dtStart = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        dtEnd = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                } catch (_: Exception) {}
            }

            val keywords = settingsManager.getAutoWorkKeywords().split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val matchedKeyword = keywords.find { eventToSave.title.lowercase().contains(it) }
            
            if (eventToSave.type == CalendarEventTypeEnum.SHIFT || matchedKeyword != null) {
                if (matchedKeyword != null && !eventToSave.note.contains(matchedKeyword, ignoreCase = true)) {
                    val newNote = if (eventToSave.note.isEmpty()) matchedKeyword else "${eventToSave.note}\n$matchedKeyword"
                    eventToSave = eventToSave.copy(note = newNote)
                }

                val workItem = WorkHistoryItem(
                    date = eventToSave.date,
                    startTime = eventToSave.startTime,
                    endTime = eventToSave.endTime ?: eventToSave.startTime,
                    mode = "CALENDAR"
                )
                dataManager.addHistoryItem(userId, workItem)
            }

            val gCalendarId = settingsManager.getGoogleCalendarId()
            val syncEnabled = settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPushEnabled()
            
            // --- NEW STRATEGY: SYNC vs UNSYNC ---
            val isShared = selectedGroupIds.isNotEmpty()
            
            if (gCalendarId.isNotEmpty() && gCalendarId != "primary" && (eventToSave.googleEventId != null || syncEnabled)) {
                try {
                    val group = groups[eventToSave.sharedGroupIds.firstOrNull()]
                    val gId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToSave, gCalendarId, group?.name)
                    if (gId != null) {
                        eventToSave = eventToSave.copy(googleEventId = gId.toString(), googleCalendarId = gCalendarId)
                    }
                } catch (e: Exception) {
                    Log.e("CalendarVM", "Google Sync failed", e)
                }
            } else if (eventToSave.googleEventId != null) {
                // USER UN-SYNCED: Delete from Google, but we will save to Firestore below
                try {
                    GoogleCalendarSyncHelper.deleteFromGoogleCalendar(context, eventToSave.googleEventId!!)
                    eventToSave = eventToSave.copy(googleEventId = null, googleCalendarId = null)
                } catch (_: Exception) {}
            }
            
            // NEW STRATEGY: 
            // 1. If SHARED: Always save to Firestore.
            // 2. If PRIVATE + SYNCED: Delete from local (already in Google).
            // 3. If PRIVATE + NOT SYNCED: Save to Firestore (jTool only).
            
            if (!isShared && eventToSave.googleEventId != null) {
                dataManager.deleteCalendarEventLocalOnly(eventToSave.id) 
            } else {
                dataManager.saveCalendarEvent(eventToSave)
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, syncResultMessage = null) }
        
        val currentMonth = _uiState.value.selectedMonth
        val fromDate = currentMonth.atDay(1)
        val toDate = currentMonth.atEndOfMonth()

        val calendarId = settingsManager.getGoogleCalendarId()
        if (calendarId.isEmpty() || calendarId == "primary") {
            _uiState.update { it.copy(isLoading = false, syncResultMessage = Loc.Calendar.selectCalendarSubtitle) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = settingsManager.getUserId()
                var pulledCount = 0
                var pushedCount = 0

                // 1. Pull Logic
                val startTime = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endTime = toDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                
                val remoteEvents = GoogleCalendarSyncHelper.pullFromGoogleCalendar(context, userId, calendarId, startTime, endTime)
                
                if (remoteEvents == null) {
                    _uiState.update { it.copy(syncResultMessage = Loc.Calendar.syncFailed.format("Google Calendar access failed.")) }
                    return@launch
                }

                // Update the memory-only Google events immediately
                _googleEvents.value = remoteEvents

                val localEvents = dataManager.allCalendarEventsOneTime(startTime, endTime)
                val remoteGoogleIds = remoteEvents.map { it.googleEventId }.toSet()

                remoteEvents.forEach { remote ->
                    val existing = localEvents.find { it.googleEventId == remote.googleEventId } 
                        ?: localEvents.find { local ->
                            local.googleEventId == null &&
                            local.title.equals(remote.title, ignoreCase = true) &&
                            local.dtStart == remote.dtStart
                        }

                    if (existing != null && existing.googleEventId == null) {
                        dataManager.saveCalendarEvent(existing.copy(googleEventId = remote.googleEventId, googleCalendarId = calendarId, type = remote.type))
                    }
                    if (existing == null) pulledCount++
                }

                // Cleanup Firestore events that are no longer in Google
                localEvents.forEach { local ->
                    if (local.googleEventId != null && !remoteGoogleIds.contains(local.googleEventId)) {
                        dataManager.deleteCalendarEvent(local.id)
                    }
                }

                // 2. Push Logic
                if (settingsManager.isGoogleCalendarPushEnabled()) {
                    val updatedLocalEvents = dataManager.allCalendarEventsOneTime(startTime, endTime)
                    updatedLocalEvents.forEach { event ->
                        if (event.googleEventId == null && event.dtStart != null) {
                            var eventToPush = event
                            val keywords = settingsManager.getAutoWorkKeywords().split(",").map { it.trim().lowercase() }
                            val matchedKeyword = keywords.find { it.isNotEmpty() && eventToPush.title.lowercase().contains(it) }
                            if (matchedKeyword != null && !eventToPush.note.contains(matchedKeyword, ignoreCase = true)) {
                                val newNote = if (eventToPush.note.isEmpty()) matchedKeyword else "${eventToPush.note}\n$matchedKeyword"
                                eventToPush = eventToPush.copy(note = newNote)
                            }

                            val gId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToPush, calendarId)
                            if (gId != null) {
                                val updated = eventToPush.copy(googleEventId = gId.toString(), googleCalendarId = calendarId)
                                dataManager.saveCalendarEvent(updated)
                                pushedCount++
                            }
                        }
                    }
                }

                _uiState.update { it.copy(syncResultMessage = "${Loc.Calendar.syncCompleted}\nPulled: $pulledCount, Pushed: $pushedCount") }
            } catch (e: Exception) {
                Log.e("CalendarVM", "Sync failed", e)
                _uiState.update { it.copy(syncResultMessage = Loc.Calendar.syncFailed.format(e.message ?: Loc.General.unknown)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncResultMessage = null) }
    }

    fun triggerAutoAddOnDayView() {
        settingsManager.setAutoOpenCalendarAdd(true)
    }
}
