package com.jtool.android.features.calendar.grid

import android.content.Context
import android.provider.CalendarContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.core.models.calendar.DayEventSummary
import com.jtool.android.core.models.calendar.EventPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class CalendarGridViewModel(
    private val context: Context,
    val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarGridUiState())
    val uiState: StateFlow<CalendarGridUiState> = _uiState.asStateFlow()

    private val _googleEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val _extensions = MutableStateFlow<Map<String, CalendarEventExtension>>(emptyMap())

    init {
        loadEvents()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadEvents() {
        val currentMonthFlow = _uiState.map { it.selectedMonth }.distinctUntilChanged()

        viewModelScope.launch {
            currentMonthFlow.collectLatest { month ->
                refreshInternal(month)
            }
        }

        combine(
            currentMonthFlow, 
            _googleEvents,
            _extensions,
            dataManager.optimisticDeletedIds,
            _uiState.map { it.showDeclinedEvents }.distinctUntilChanged()
        ) { _, googleEvents, extensions, deletedIds, showDeclined ->
            val authEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim() ?: ""
            val userEmail = settingsManager.getGoogleCalendarEmail().ifEmpty { authEmail }.lowercase().trim()

            val processedGoogleEvents = googleEvents.map { event ->
                val ext = extensions[event.id] ?: extensions[event.iCalUID] ?: extensions.values.find { it.googleEventId == event.id || it.googleEventId == event.iCalUID }
                val extStatus = ext?.attendeeStatuses?.get(userEmail) ?: ext?.attendeeStatuses?.get(authEmail)
                val effectiveStatus = extStatus ?: event.currentUserStatus ?: event.attendeeStatuses[userEmail] ?: event.attendeeStatuses[authEmail]
                
                val colorKey = event.colorKey?.takeIf { !it.isNullOrEmpty() } ?: ext?.colorKey ?: "7"
                val title = if (!event.title.isNullOrEmpty() && event.title != Loc.Calendar.event) event.title else (ext?.title?.takeIf { !it.isNullOrEmpty() } ?: event.title.ifEmpty { Loc.Calendar.event })
                
                if (ext != null && (ext.title.isNullOrEmpty() || ext.colorKey.isNullOrEmpty())) {
                    val updatedExt = ext.copy(
                        title = title,
                        colorKey = colorKey
                    )
                    viewModelScope.launch {
                        dataManager.saveCalendarExtension(updatedExt)
                    }
                }
                
                event.copy(
                    currentUserStatus = effectiveStatus,
                    colorKey = colorKey,
                    title = title
                )
            }

            val filtered = processedGoogleEvents.filter { event ->
                if (deletedIds.contains(event.id)) return@filter false
                if (!showDeclined && event.currentUserStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) return@filter false
                true
            }

            val existingIds = googleEvents.mapNotNull { it.iCalUID }.toSet() + googleEvents.map { it.id }.toSet()
            val extraEvents = mutableListOf<CalendarEvent>()

            extensions.forEach { (extId, ext) ->
                if (!existingIds.contains(extId) && !existingIds.contains(ext.googleEventId) && ext.dateTimestamp != 0L) {
                    val extStatus = ext.attendeeStatuses[userEmail] ?: ext.attendeeStatuses[authEmail] ?: CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
                    if (!showDeclined && extStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) {
                        // Skip
                    } else if (ext.title.isNullOrEmpty() && ext.sharedGroupIds.isEmpty()) {
                        viewModelScope.launch {
                            dataManager.deleteCalendarExtension(extId)
                            dataManager.deleteCalendarExtension(ext.googleEventId)
                        }
                    } else {
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

            (filtered + extraEvents) to extensions
        }.onEach { (mergedEvents, extensions) ->
            val groups = settingsManager.getGroups()
            val summaries = mergedEvents.groupBy { it.date }.mapValues { (_, dayEvents) ->
                DayEventSummary(
                    hasEvents = dayEvents.isNotEmpty(),
                    eventCount = dayEvents.size,
                    eventColorKeys = dayEvents.map { it.colorKey }.take(4),
                    eventPreviews = dayEvents.take(2).map { evt ->
                        val ext = extensions[evt.id] ?: extensions[evt.iCalUID] ?: extensions.values.find { it.googleEventId == evt.id || it.googleEventId == evt.iCalUID }
                        val groupTagName = ext?.sharedGroupIds?.firstOrNull()?.let { gid ->
                            val group = groups[gid]
                            if (group?.isPrivate == true) Loc.Calendar.private else group?.name ?: gid
                        }
                        EventPreview(
                            id = evt.id,
                            title = evt.title,
                            colorKey = evt.colorKey,
                            isDeclined = evt.currentUserStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
                            groupTagName = groupTagName
                        )
                    }
                )
            }
            
            _uiState.update { it.copy(
                privateEvents = mergedEvents.sortedBy { it.dtStart ?: 0L }, 
                eventSummaries = summaries,
                extensions = extensions,
                isLoading = false
            ) }
        }.launchIn(viewModelScope)
    }

    private fun refreshInternal(month: YearMonth) {
        val calendarId = settingsManager.getGoogleCalendarId()
        val sharedCalendarId = settingsManager.getGoogleCalendarSharedId()
        
        if (calendarId.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        
        val start = month.atDay(1).minusWeeks(1)
        val end = month.atEndOfMonth().plusWeeks(1)
        val startMs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val userEmail = settingsManager.getGoogleCalendarEmail()

        viewModelScope.launch {
            val displayedIds = settingsManager.getDisplayedCalendarIds()
            val mainEvents = GoogleCalendarSyncHelper.pullFromMultipleGoogleCalendars(context, displayedIds, startMs, endMs, userEmail)
            
            val allEvents = if (sharedCalendarId.isNotEmpty() && sharedCalendarId != calendarId) {
                val sharedEvents = GoogleCalendarSyncHelper.pullFromGoogleCalendar(context, sharedCalendarId, startMs, endMs, userEmail) ?: emptyList()
                (mainEvents + sharedEvents).distinctBy { it.iCalUID ?: it.id }
            } else {
                mainEvents
            }
            
            _googleEvents.value = allEvents
            
            val extensions = dataManager.getAllCalendarExtensions()
            
            if (allEvents.isNotEmpty()) {
                dataManager.reconcileOptimisticDeletions(allEvents.map { it.id }.toSet())
            }
            
            val mappedExtensions = mutableMapOf<String, CalendarEventExtension>()
            allEvents.forEach { event ->
                val ext = extensions[event.id] 
                    ?: extensions[event.iCalUID] 
                    ?: extensions.values.find { it.googleEventId == event.id || it.googleEventId == event.iCalUID }
                if (ext != null) {
                    mappedExtensions[event.id] = ext
                    if (event.iCalUID != null) mappedExtensions[event.iCalUID] = ext
                    mappedExtensions[ext.googleEventId] = ext
                }
            }
            // Also include extensions that might not have matching google events yet (invited / extra events)
            extensions.forEach { (k, v) ->
                mappedExtensions[k] = v
                mappedExtensions[v.googleEventId] = v
            }
            _extensions.value = mappedExtensions
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onDateSelected(date: LocalDate) {
        settingsManager.setSelectedCalendarDate(date.toString())
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onEventClicked(event: CalendarEvent) {
        settingsManager.setSelectedCalendarDate(event.date)
        settingsManager.setSelectedCalendarEventId(event.id)
    }

    fun onMonthChange(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun toggleAddEditDialog(show: Boolean, event: CalendarEvent? = null) {
        if (show && event == null) {
            settingsManager.setSelectedCalendarEventId(null)
        }
        _uiState.update { it.copy(showAddEditDialog = show, editingEvent = event) }
    }

    fun prepareEdit(eventId: String) {
        val event = _googleEvents.value.find { it.id == eventId || it.iCalUID == eventId }
        if (event != null) {
            _uiState.update { it.copy(editingEvent = event) }
        } else {
            viewModelScope.launch {
                val pulled = GoogleCalendarSyncHelper.pullEventById(context, eventId, settingsManager.getGoogleCalendarEmail())
                if (pulled != null) {
                    _uiState.update { it.copy(editingEvent = pulled) }
                }
            }
        }
    }

    fun addEvent(context: Context, event: CalendarEvent, extension: CalendarEventExtension) {
        viewModelScope.launch {
            val calendarId = settingsManager.getGoogleCalendarId()
            if (calendarId.isEmpty()) return@launch

            val attendees = mutableSetOf<String>()
            extension.sharedGroupIds.forEach { gid ->
                attendees.addAll(dataManager.getGroupMemberEmails(gid))
            }
            
            val eventToSave = event.copy(attendees = attendees.toList())
            val googleEventId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToSave, calendarId, settingsManager.getGoogleCalendarEmail())
            
            if (googleEventId != null) {
                val userEmail = settingsManager.getGoogleCalendarEmail().ifEmpty { FirebaseAuth.getInstance().currentUser?.email ?: "" }.lowercase().trim()
                val statuses = extension.attendeeStatuses.toMutableMap()
                if (userEmail.isNotEmpty()) statuses[userEmail] = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
                dataManager.saveCalendarExtension(extension.copy(
                    googleEventId = googleEventId,
                    dateTimestamp = event.dtStart ?: 0L,
                    ownerUid = settingsManager.getUserId(),
                    attendeeStatuses = statuses,
                    colorKey = event.colorKey,
                    title = event.title
                ))
                extension.sharedGroupIds.forEach { gid ->
                    dataManager.sendEventNotification(gid, event.title, googleEventId)
                }
            }
            
            refresh()
        }
    }

    fun toggleShowDeclinedEvents() {
        _uiState.update { it.copy(showDeclinedEvents = !it.showDeclinedEvents) }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            val event = _googleEvents.value.find { eventId.contains(it.id) || (it.iCalUID != null && eventId.contains(it.iCalUID)) }
            val searchId = event?.iCalUID ?: event?.id ?: eventId
            val ext = _extensions.value[searchId] ?: _extensions.value[eventId] ?: _extensions.value.values.find { it.googleEventId == eventId || it.googleEventId == searchId }
            
            val myUid = settingsManager.getUserId()
            val authEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val userEmail = settingsManager.getGoogleCalendarEmail().ifEmpty { authEmail }.lowercase().trim()
            val isOwner = (ext != null && ext.ownerUid == myUid) || (event != null && event.organizer != null && event.organizer.lowercase().trim().let { it == userEmail || it == authEmail.lowercase().trim() })

            dataManager.recordOptimisticDelete(eventId)

            if (isOwner) {
                GoogleCalendarSyncHelper.deleteFromGoogleCalendar(context, eventId)
                event?.id?.let { GoogleCalendarSyncHelper.deleteFromGoogleCalendar(context, it) }
            }

            ext?.sharedGroupIds?.forEach { gid ->
                dataManager.sendEventDeletedNotification(gid, event?.title ?: ext.title ?: "Event")
            }

            dataManager.deleteCalendarExtension(eventId)
            dataManager.deleteCalendarExtension(eventId.removePrefix("ext_"))
            dataManager.deleteCalendarExtension(searchId)
            ext?.googleEventId?.let { dataManager.deleteCalendarExtension(it) }
            event?.id?.let { dataManager.deleteCalendarExtension(it) }
            event?.iCalUID?.let { dataManager.deleteCalendarExtension(it) }

            dataManager.markEventNotificationsAsRead(eventId)
            event?.iCalUID?.let { dataManager.markEventNotificationsAsRead(it) }
            ext?.googleEventId?.let { dataManager.markEventNotificationsAsRead(it) }

            delay(500)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            dataManager.clearOptimisticDeletions()
            refreshInternal(_uiState.value.selectedMonth)
        }
    }
}
