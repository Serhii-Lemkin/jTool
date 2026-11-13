package com.jtool.android.features.calendar.detail

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.user.PublicProfile
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.helpers.FirestoreCommentRepository
import com.jtool.android.core.components.comments.CommentController
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CalendarEventDetailViewModel(
    val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarEventDetailUiState())
    val uiState: StateFlow<CalendarEventDetailUiState> = _uiState.asStateFlow()

    private val commentController = CommentController(FirestoreCommentRepository(), viewModelScope) { settingsManager.getUserId() }
    val comments = commentController.comments

    private var eventJob: Job? = null
    private var extensionJob: Job? = null

    init {
        loadEvent()
        observeUserNames()
        observeCommentsState()
    }

    private fun observeUserNames() {
        dataManager.userNamesFlow()
            .onEach { names -> 
                val updatedNames = names.toMutableMap()
                val myName = settingsManager.getUserName()
                val myUid = settingsManager.getUserId()
                if (myName.isNotEmpty()) updatedNames[myUid] = myName
                _uiState.update { it.copy(userNames = updatedNames) } 
            }
            .launchIn(viewModelScope)
    }

    private fun observeCommentsState() {
        commentController.comments
            .onEach { updatedComments -> _uiState.update { it.copy(comments = updatedComments) } }
            .launchIn(viewModelScope)
    }

    val emailToProfile = dataManager.publicProfilesFlow().map { profiles ->
        val map = mutableMapOf<String, PublicProfile>()
        profiles.values.forEach { profile ->
            if (!profile.email.isNullOrEmpty()) {
                map[profile.email.lowercase().trim()] = profile
            }
            if (!profile.calendarEmail.isNullOrEmpty()) {
                map[profile.calendarEmail.lowercase().trim()] = profile
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun refresh() {
        loadEvent()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadEvent() {
        eventJob?.cancel()
        val eventId = settingsManager.getSelectedCalendarEventId() ?: return
        val rawId = eventId.removePrefix("ext_")
        
        _uiState.update { it.copy(event = null, isLoading = true, isPermissionDenied = false) }

        val userEmail = settingsManager.getGoogleCalendarEmail()

        viewModelScope.launch {
            var event = GoogleCalendarSyncHelper.pullEventById(dataManager.context, rawId, userEmail)
                ?: GoogleCalendarSyncHelper.pullEventById(dataManager.context, eventId, userEmail)
            
            val searchId = event?.iCalUID ?: event?.id ?: rawId
            val extension = dataManager.getCalendarExtension(searchId)
                ?: dataManager.getCalendarExtension(rawId)
                ?: dataManager.getCalendarExtension(eventId)

            if (event == null) {
                val anchorMs = extension?.dateTimestamp?.takeIf { it > 0L } ?: run {
                    try {
                        LocalDate.parse(settingsManager.getSelectedCalendarDate())
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                }
                val startMs = anchorMs - 90L * 24 * 60 * 60 * 1000
                val endMs = anchorMs + 180L * 24 * 60 * 60 * 1000

                val calIds = (settingsManager.getDisplayedCalendarIds() +
                    settingsManager.getGoogleCalendarId() +
                    settingsManager.getGoogleCalendarSharedId() +
                    GoogleCalendarSyncHelper.getAvailableCalendars(dataManager.context).map { it.first })
                    .filter { it.isNotEmpty() }
                    .toSet()

                val allEvents = if (calIds.isNotEmpty()) {
                    GoogleCalendarSyncHelper.pullFromMultipleGoogleCalendars(
                        dataManager.context,
                        calIds,
                        startMs,
                        endMs,
                        userEmail
                    )
                } else emptyList()

                event = allEvents.find {
                    it.id == eventId ||
                    it.id == rawId ||
                    it.iCalUID == eventId ||
                    it.iCalUID == rawId ||
                    (extension != null && (
                        it.id == extension.googleEventId ||
                        it.iCalUID == extension.googleEventId ||
                        (extension.localEventId.isNotEmpty() && it.id == extension.localEventId)
                    )) ||
                    it.note.contains("/event/$rawId")
                }
            }

            _uiState.update { it.copy(extension = extension ?: CalendarEventExtension(googleEventId = searchId)) }

            if (event != null) {
                val memberEmails = mutableSetOf<String>()
                memberEmails.addAll(event.attendees)
                extension?.sharedGroupIds?.forEach { gid ->
                    try {
                        val emails = dataManager.getGroupMemberEmails(gid)
                        memberEmails.addAll(emails)
                    } catch (_: Exception) {}
                }
                val fullAttendees = memberEmails.map { it.lowercase().trim() }.distinct().filter { it.isNotEmpty() }
                val colorKey = event.colorKey?.takeIf { !it.isNullOrEmpty() } ?: extension?.colorKey ?: "7"
                val title = if (!event.title.isNullOrEmpty() && event.title != Loc.Calendar.event) event.title else (extension?.title?.takeIf { !it.isNullOrEmpty() } ?: event.title)

                val updatedEvent = event.copy(attendees = fullAttendees, colorKey = colorKey, title = title)
                _uiState.update { it.copy(event = updatedEvent, isPermissionDenied = false, isLoading = false) }
            } else if (extension != null) {
                val memberEmails = mutableSetOf<String>()
                extension.sharedGroupIds.forEach { gid ->
                    try {
                        val emails = dataManager.getGroupMemberEmails(gid)
                        memberEmails.addAll(emails)
                    } catch (_: Exception) {}
                }
                val fullAttendees = memberEmails.map { it.lowercase().trim() }.distinct().filter { it.isNotEmpty() }
                val startMs = if (extension.dateTimestamp != 0L) extension.dateTimestamp else System.currentTimeMillis()
                val placeholder = CalendarEvent(
                    id = extension.googleEventId.ifEmpty { rawId },
                    title = extension.title?.takeIf { it.isNotEmpty() } ?: "Shared Event",
                    dtStart = startMs,
                    dtEnd = if (extension.endTimestamp > startMs) extension.endTimestamp else startMs + 3600_000L,
                    attendees = fullAttendees,
                    currentUserStatus = CalendarContract.Attendees.ATTENDEE_STATUS_INVITED,
                    iCalUID = extension.googleEventId,
                    colorKey = extension.colorKey ?: "7"
                )
                _uiState.update { it.copy(event = placeholder, isPermissionDenied = false, isLoading = false) }
            } else {
                _uiState.update { it.copy(event = null, isLoading = false) }
            }
            
            commentController.observe(dataManager.calendarCommentsFlow(searchId))

            extensionJob?.cancel()
            extensionJob = dataManager.calendarExtensionFlow(searchId)
                .onEach { freshExt ->
                    if (freshExt != null) {
                        _uiState.update { state ->
                            val currentEvt = state.event
                            val updatedEvt = currentEvt?.let { evt ->
                                val mergedStatuses = evt.attendeeStatuses.toMutableMap()
                                freshExt.attendeeStatuses.forEach { (email, status) ->
                                    mergedStatuses[email.lowercase().trim()] = status
                                }
                                val myEmail = settingsManager.getGoogleCalendarEmail().ifEmpty { 
                                    FirebaseAuth.getInstance().currentUser?.email ?: ""
                                }.lowercase().trim()
                                val myAuthEmail = FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim() ?: ""
                                
                                val extStatus = freshExt.attendeeStatuses[myEmail] ?: freshExt.attendeeStatuses[myAuthEmail]
                                val myStatus = extStatus ?: evt.currentUserStatus

                                val colorKey = evt.colorKey?.takeIf { !it.isNullOrEmpty() } ?: freshExt.colorKey ?: "7"
                                val title = if (!evt.title.isNullOrEmpty() && evt.title != Loc.Calendar.event) evt.title else (freshExt.title?.takeIf { !it.isNullOrEmpty() } ?: evt.title)

                                evt.copy(
                                    currentUserStatus = myStatus,
                                    attendeeStatuses = mergedStatuses,
                                    colorKey = colorKey,
                                    title = title
                                )
                            }
                            state.copy(extension = freshExt, event = updatedEvt ?: state.event)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun addComment(content: String, parentId: String? = null) {
        commentController.addComment(content, parentId)
        val event = uiState.value.event ?: return
        val ext = uiState.value.extension
        val searchId = event.iCalUID ?: event.id
        val newCount = uiState.value.comments.size + 1

        viewModelScope.launch {
            ext?.sharedGroupIds?.forEach { gid ->
                dataManager.sendEventCommentNotification(gid, event.title, searchId, newCount)
            }
        }
    }

    fun acceptInvite() {
        val event = uiState.value.event ?: return
        val ext = uiState.value.extension
        val authEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val userEmail = settingsManager.getGoogleCalendarEmail().ifEmpty { authEmail }.lowercase().trim()
        if (userEmail.isEmpty()) return
        
        val updatedStatuses = event.attendeeStatuses.toMutableMap()
        updatedStatuses[userEmail] = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
        if (authEmail.isNotEmpty()) {
            updatedStatuses[authEmail.lowercase().trim()] = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
        }
        val updatedEvent = event.copy(
            currentUserStatus = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            attendeeStatuses = updatedStatuses
        )
        _uiState.update { it.copy(event = updatedEvent, isResponding = true) }

        viewModelScope.launch {
            try {
                GoogleCalendarSyncHelper.updateAttendeeStatus(dataManager.context, event.id, userEmail, CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED)
                if (authEmail.isNotEmpty() && authEmail != userEmail) {
                    GoogleCalendarSyncHelper.updateAttendeeStatus(dataManager.context, event.id, authEmail, CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED)
                }

                val googleEventId = (event.iCalUID ?: event.id).removePrefix("ext_")
                val currentExt = ext ?: CalendarEventExtension(googleEventId = googleEventId)
                val extStatuses = currentExt.attendeeStatuses.toMutableMap()
                extStatuses[userEmail] = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
                if (authEmail.isNotEmpty()) extStatuses[authEmail.lowercase().trim()] = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
                dataManager.saveCalendarExtension(currentExt.copy(
                    googleEventId = googleEventId,
                    attendeeStatuses = extStatuses,
                    dateTimestamp = event.dtStart ?: 0L,
                    colorKey = event.colorKey,
                    title = event.title
                ))
                
                currentExt.sharedGroupIds.forEach { gid ->
                    dataManager.sendEventResponseNotification(gid, event.title, true, googleEventId)
                }
                dataManager.markEventNotificationsAsRead(event.id)
                event.iCalUID?.let { dataManager.markEventNotificationsAsRead(it) }
            } catch (_: Exception) {} finally {
                _uiState.update { it.copy(isResponding = false) }
                refresh()
            }
        }
    }

    fun declineInvite() {
        val event = uiState.value.event ?: return
        val ext = uiState.value.extension
        val authEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val userEmail = settingsManager.getGoogleCalendarEmail().ifEmpty { authEmail }.lowercase().trim()
        if (userEmail.isEmpty()) return
        
        val updatedStatuses = event.attendeeStatuses.toMutableMap()
        updatedStatuses[userEmail] = CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
        if (authEmail.isNotEmpty()) {
            updatedStatuses[authEmail.lowercase().trim()] = CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
        }
        val updatedEvent = event.copy(
            currentUserStatus = CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
            attendeeStatuses = updatedStatuses
        )
        _uiState.update { it.copy(event = updatedEvent, isResponding = true) }

        viewModelScope.launch {
            try {
                GoogleCalendarSyncHelper.updateAttendeeStatus(dataManager.context, event.id, userEmail, CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED)
                if (authEmail.isNotEmpty() && authEmail != userEmail) {
                    GoogleCalendarSyncHelper.updateAttendeeStatus(dataManager.context, event.id, authEmail, CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED)
                }

                val googleEventId = (event.iCalUID ?: event.id).removePrefix("ext_")
                val currentExt = ext ?: CalendarEventExtension(googleEventId = googleEventId)
                val extStatuses = currentExt.attendeeStatuses.toMutableMap()
                extStatuses[userEmail] = CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                if (authEmail.isNotEmpty()) extStatuses[authEmail.lowercase().trim()] = CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                dataManager.saveCalendarExtension(currentExt.copy(
                    googleEventId = googleEventId,
                    attendeeStatuses = extStatuses,
                    dateTimestamp = event.dtStart ?: 0L,
                    colorKey = event.colorKey,
                    title = event.title
                ))
                
                currentExt.sharedGroupIds.forEach { gid ->
                    dataManager.sendEventResponseNotification(gid, event.title, false, googleEventId)
                }
                dataManager.markEventNotificationsAsRead(event.id)
                event.iCalUID?.let { dataManager.markEventNotificationsAsRead(it) }
            } catch (_: Exception) {} finally {
                _uiState.update { it.copy(isResponding = false) }
                refresh()
            }
        }
    }

    fun toggleEditDialog(show: Boolean) {
        _uiState.update { it.copy(showEditDialog = show) }
    }

    fun updateEvent(context: Context, updatedEvent: CalendarEvent, selectedGroupIds: List<String>) {
        viewModelScope.launch {
            val calendarId = settingsManager.getGoogleCalendarId()
            if (calendarId.isEmpty()) return@launch

            val attendees = mutableSetOf<String>()
            selectedGroupIds.forEach { gid ->
                attendees.addAll(dataManager.getGroupMemberEmails(gid))
            }
            
            val eventToSave = updatedEvent.copy(attendees = attendees.toList())
            val globalId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToSave, calendarId, settingsManager.getGoogleCalendarEmail())
            
            if (globalId != null) {
                val currentExt = uiState.value.extension ?: CalendarEventExtension(googleEventId = globalId)
                dataManager.saveCalendarExtension(currentExt.copy(
                    googleEventId = globalId,
                    sharedGroupIds = selectedGroupIds,
                    dateTimestamp = updatedEvent.dtStart ?: 0L,
                    colorKey = updatedEvent.colorKey,
                    title = updatedEvent.title
                ))
            }

            _uiState.update { it.copy(event = eventToSave, showEditDialog = false) }
            refresh()
        }
    }

    fun deleteEvent() {
        val event = uiState.value.event ?: return
        val ext = uiState.value.extension
        val searchId = event.iCalUID ?: event.id

        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        dataManager.recordOptimisticDelete(event.id)
        viewModelScope.launch {
            try {
                var success = GoogleCalendarSyncHelper.deleteFromGoogleCalendar(dataManager.context, event.id)
                if (!success && !event.iCalUID.isNullOrEmpty()) {
                    success = GoogleCalendarSyncHelper.deleteFromGoogleCalendar(dataManager.context, event.iCalUID)
                }
                if (!success && !ext?.localEventId.isNullOrEmpty()) {
                    success = GoogleCalendarSyncHelper.deleteFromGoogleCalendar(dataManager.context, ext!!.localEventId)
                }
                if (!success && !ext?.googleEventId.isNullOrEmpty()) {
                    success = GoogleCalendarSyncHelper.deleteFromGoogleCalendar(dataManager.context, ext!!.googleEventId)
                }

                val isVirtual = event.id.startsWith("ext_") || event.id.startsWith("virtual_")
                val verifiedDeleted = isVirtual || (
                    GoogleCalendarSyncHelper.isEventDeleted(dataManager.context, event.id, ext?.localEventId ?: "") &&
                    (event.iCalUID.isNullOrEmpty() || GoogleCalendarSyncHelper.isEventDeleted(dataManager.context, event.iCalUID))
                )

                if (verifiedDeleted) {
                    ext?.sharedGroupIds?.forEach { gid ->
                        dataManager.sendEventDeletedNotification(gid, event.title)
                    }
                    dataManager.deleteCalendarExtension(searchId)
                    dataManager.deleteCalendarExtension(event.id)
                    ext?.googleEventId?.takeIf { it.isNotEmpty() }?.let { dataManager.deleteCalendarExtension(it) }
                    ext?.localEventId?.takeIf { it.isNotEmpty() }?.let { dataManager.deleteCalendarExtension(it) }
                    dataManager.markEventNotificationsAsRead(event.id)
                    event.iCalUID?.let { dataManager.markEventNotificationsAsRead(it) }
                    ext?.googleEventId?.let { dataManager.markEventNotificationsAsRead(it) }
                    _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
                } else {
                    dataManager.clearOptimisticDelete(event.id)
                    _uiState.update { it.copy(isDeleting = false, deleteError = Loc.General.unknown) }
                }
            } catch (e: Exception) {
                dataManager.clearOptimisticDelete(event.id)
                _uiState.update { it.copy(isDeleting = false, deleteError = e.message ?: Loc.General.unknown) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        commentController.stop()
    }
}
