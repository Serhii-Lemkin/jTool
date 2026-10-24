package com.jtool.android.features.calendar.day

import android.content.Context
import android.provider.CalendarContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
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
    private val _extensions = MutableStateFlow<Map<String, CalendarEventExtension>>(emptyMap())

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
        dataManager.clearOptimisticDeletions()
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
        val startMs = date.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val calendarId = settingsManager.getGoogleCalendarId()
            val sharedCalendarId = settingsManager.getGoogleCalendarSharedId()
            val userEmail = settingsManager.getGoogleCalendarEmail()

            val displayedIds = settingsManager.getDisplayedCalendarIds()

            if (displayedIds.isNotEmpty()) {
                val mainEvents = GoogleCalendarSyncHelper.pullFromMultipleGoogleCalendars(dataManager.context, displayedIds, startMs, endMs, userEmail)
                
                val allEvents = if (sharedCalendarId.isNotEmpty() && sharedCalendarId != calendarId) {
                    val sharedEvents = GoogleCalendarSyncHelper.pullFromGoogleCalendar(dataManager.context, sharedCalendarId, startMs, endMs, userEmail) ?: emptyList()
                    (mainEvents + sharedEvents).distinctBy { it.iCalUID ?: it.id }
                } else {
                    mainEvents
                }
                
                _googleEvents.value = allEvents
                
                val userId = settingsManager.getUserId()
                val groupIds = settingsManager.getGroups().keys.toList()
                val exts = dataManager.getCalendarExtensionsForRange(userId, groupIds, startMs, endMs)
                val mappedExtensions = mutableMapOf<String, CalendarEventExtension>()
                allEvents.forEach { event ->
                    val ext = exts[event.id] 
                        ?: exts[event.iCalUID] 
                        ?: exts.values.find { it.googleEventId == event.id || it.googleEventId == event.iCalUID }
                    if (ext != null) {
                        mappedExtensions[event.id] = ext
                        if (event.iCalUID != null) mappedExtensions[event.iCalUID] = ext
                        mappedExtensions[ext.googleEventId] = ext
                    }
                }
                exts.forEach { (k, v) ->
                    mappedExtensions[k] = v
                    mappedExtensions[v.googleEventId] = v
                }
                _extensions.value = mappedExtensions
            } else {
                _googleEvents.value = emptyList()
                _extensions.value = emptyMap()
            }
        }

        combine(
            _googleEvents, 
            _extensions,
            dataManager.optimisticDeletedIds,
            _uiState.map { it.showDeclinedEvents }.distinctUntilChanged()
        ) { googleEvents, extensions, deletedIds, showDeclined ->
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
        }.onEach { (allEvents, extensions) ->
            val displayEvents = mutableListOf<CalendarEvent>()
            
            allEvents.forEach { event ->
                val startMs = event.dtStart
                val endMs = event.dtEnd
                
                val eventStartLdt = if (startMs != null) {
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault())
                } else {
                    LocalDateTime.now()
                }
                
                val eventDate = eventStartLdt.toLocalDate()
                
                val eventEndLdt = if (endMs != null) {
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(endMs), ZoneId.systemDefault())
                } else {
                    eventStartLdt.plusHours(1)
                }
                
                val isOvernight = eventEndLdt.toLocalDate().isAfter(eventDate)

                if (eventDate == date) {
                    displayEvents.add(event)
                } else if (eventDate == date.minusDays(1) && isOvernight) {
                    displayEvents.add(event.copy(
                        dtStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        note = "Continued from yesterday. ${event.note}".trim(),
                        id = "${event.id}_cont"
                    ))
                }
            }

            _uiState.update { it.copy(
                privateEvents = displayEvents.sortedBy { it.dtStart ?: 0L },
                extensions = extensions
            ) }
        }.launchIn(viewModelScope)
    }

    fun toggleAddEditDialog(show: Boolean, event: CalendarEvent? = null) {
        if (event != null) {
            settingsManager.setSelectedCalendarEventId(event.id)
        }
        _uiState.update { it.copy(showAddEditDialog = show, editingEvent = event) }
    }

    fun addEvent(context: Context, event: CalendarEvent, selectedGroupIds: List<String>) {
        viewModelScope.launch {
            val calendarId = settingsManager.getGoogleCalendarId()
            if (calendarId.isEmpty()) return@launch

            val attendees = mutableSetOf<String>()
            selectedGroupIds.forEach { gid ->
                attendees.addAll(dataManager.getGroupMemberEmails(gid))
            }
            
            val eventToSave = event.copy(attendees = attendees.toList())
            val googleEventId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToSave, calendarId, settingsManager.getGoogleCalendarEmail())
            
            if (googleEventId != null) {
                val userEmail = settingsManager.getGoogleCalendarEmail().ifEmpty { FirebaseAuth.getInstance().currentUser?.email ?: "" }.lowercase().trim()
                val statuses = mutableMapOf<String, Int>()
                if (userEmail.isNotEmpty()) statuses[userEmail] = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED

                val extension = CalendarEventExtension(
                    googleEventId = googleEventId,
                    sharedGroupIds = selectedGroupIds,
                    dateTimestamp = event.dtStart ?: 0L,
                    ownerUid = settingsManager.getUserId(),
                    attendeeStatuses = statuses,
                    colorKey = event.colorKey,
                    title = event.title
                )
                dataManager.saveCalendarExtension(extension)
                
                selectedGroupIds.forEach { gid ->
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
                GoogleCalendarSyncHelper.deleteFromGoogleCalendar(dataManager.context, eventId)
                event?.id?.let { GoogleCalendarSyncHelper.deleteFromGoogleCalendar(dataManager.context, it) }
            }

            ext?.sharedGroupIds?.forEach { gid ->
                dataManager.sendEventDeletedNotification(gid, event?.title ?: ext.title ?: "Event")
            }

            dataManager.deleteCalendarExtension(eventId)
            dataManager.deleteCalendarExtension(eventId.removePrefix("ext_"))
            dataManager.deleteCalendarExtension(searchId)
            ext?.googleEventId?.let { dataManager.deleteCalendarExtension(it) }
            event?.id?.let { dataManager.deleteCalendarExtension(it) }
            event?.iCalUID?.let { dataManager.markEventNotificationsAsRead(it) }

            dataManager.markEventNotificationsAsRead(eventId)
            event?.iCalUID?.let { dataManager.markEventNotificationsAsRead(it) }
            ext?.googleEventId?.let { dataManager.markEventNotificationsAsRead(it) }

            refresh()
        }
    }
}
