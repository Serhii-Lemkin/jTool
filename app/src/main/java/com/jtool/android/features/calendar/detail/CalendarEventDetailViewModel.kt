package com.jtool.android.features.calendar.detail

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.components.comments.CommentController
import com.jtool.android.core.components.comments.CommentEvent
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.common.Comment
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.helpers.CommentRepository
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CalendarEventDetailViewModel(
    val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarEventDetailUiState())
    val uiState: StateFlow<CalendarEventDetailUiState> = _uiState.asStateFlow()

    private val _googleEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private var eventJob: Job? = null

    private val commentController = CommentController(
        repository = object : CommentRepository {
            override fun getCommentsFlow(collection: CollectionReference): Flow<List<Comment>> {
                val eventId = _uiState.value.event?.id ?: return flowOf(emptyList())
                return dataManager.calendarCommentsFlow(eventId)
            }

            override suspend fun addComment(collection: CollectionReference, comment: Comment) {
                val eventId = _uiState.value.event?.id ?: return
                dataManager.addCalendarComment(eventId, comment)
            }

            override suspend fun updateComment(collection: CollectionReference, commentId: String, content: String) {
                val eventId = _uiState.value.event?.id ?: return
                dataManager.updateCalendarComment(eventId, commentId, content)
            }

            override suspend fun deleteComment(collection: CollectionReference, commentId: String) {
                val eventId = _uiState.value.event?.id ?: return
                dataManager.deleteCalendarComment(eventId, commentId)
            }
        },
        scope = viewModelScope,
        currentUserIdProvider = { settingsManager.getUserId() }
    )

    val comments = commentController.comments
    val userNames = dataManager.userNamesFlow()

    init {
        loadEvent()
    }

    fun refresh() {
        loadEvent()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadEvent() {
        eventJob?.cancel()
        val eventId = settingsManager.getSelectedCalendarEventId() ?: return
        
        val dateStr = settingsManager.getSelectedCalendarDate()
        val date = LocalDate.parse(dateStr)
        val startMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 1. Fetch Google Events for this day to support "Google-only" events
        viewModelScope.launch {
            if (settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPullEnabled()) {
                val calendarId = settingsManager.getGoogleCalendarId()
                if (calendarId.isNotEmpty() && calendarId != "primary") {
                    val context = dataManager.context
                    val events = GoogleCalendarSyncHelper.pullFromGoogleCalendar(context, settingsManager.getUserId(), calendarId, startMs, endMs)
                    _googleEvents.value = events ?: emptyList()
                }
            }
        }

        // 2. Combine Firestore and Google Events to find the target event
        eventJob = combine(dataManager.allCalendarEventsFlow(startMs, endMs), _googleEvents) { firestoreEvents, googleEvents ->
            (firestoreEvents + googleEvents).find { it.id == eventId || it.googleEventId == eventId || (it.googleEventId != null && eventId.endsWith(it.googleEventId!!)) }
        }.onEach { event ->
            _uiState.update { it.copy(event = event) }
        }.launchIn(viewModelScope)
    }

    fun onCommentEvent(event: CommentEvent) {
        when (event) {
            is CommentEvent.Add -> commentController.addComment(event.content, event.parentId)
            is CommentEvent.Edit -> commentController.editComment(event.commentId, event.newContent)
            is CommentEvent.Delete -> commentController.deleteComment(event.commentId)
        }
    }

    fun toggleEditDialog(show: Boolean) {
        _uiState.update { it.copy(showEditDialog = show) }
    }

    fun updateEvent(context: Context, updatedEvent: CalendarEvent, selectedGroupIds: List<String>) {
        viewModelScope.launch {
            var eventToSave = updatedEvent.copy(sharedGroupIds = selectedGroupIds)

            // Keyword Detection
            val keywords = settingsManager.getAutoWorkKeywords().split(",").map { it.trim().lowercase() }
            val matchedKeyword = keywords.find { eventToSave.title.lowercase().contains(it) }
            if (matchedKeyword != null && !eventToSave.note.contains(matchedKeyword, ignoreCase = true)) {
                val newNote = if (eventToSave.note.isEmpty()) matchedKeyword else "${eventToSave.note}\n$matchedKeyword"
                eventToSave = eventToSave.copy(note = newNote)
            }

            // Google Sync
            val gCalendarId = settingsManager.getGoogleCalendarId()
            val syncEnabled = settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPushEnabled()

            if (gCalendarId.isNotEmpty() && gCalendarId != "primary" && (eventToSave.googleEventId != null || syncEnabled)) {
                try {
                    val groups = settingsManager.getGroups()
                    val group = groups[eventToSave.sharedGroupIds.firstOrNull()]
                    val gId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToSave, gCalendarId, group?.name)
                    if (gId != null) {
                        eventToSave = eventToSave.copy(googleEventId = gId.toString(), googleCalendarId = gCalendarId)
                    }
                } catch (e: Exception) {
                    Log.e("CalendarDetailVM", "Google Sync failed", e)
                }
            } else if (eventToSave.googleEventId != null) {
                // USER UN-SYNCED: Delete from Google
                try {
                    GoogleCalendarSyncHelper.deleteFromGoogleCalendar(context, eventToSave.googleEventId!!)
                    eventToSave = eventToSave.copy(googleEventId = null, googleCalendarId = null)
                } catch (_: Exception) {}
            }

            // GOOGLE-FIRST STRATEGY:
            if (eventToSave.sharedGroupIds.isEmpty() && eventToSave.googleEventId != null) {
                dataManager.deleteCalendarEventLocalOnly(eventToSave.id)
            } else {
                dataManager.saveCalendarEvent(eventToSave)
            }
            
            _uiState.update { it.copy(event = eventToSave, showEditDialog = false) }
        }
    }

    fun deleteEvent() {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            dataManager.deleteCalendarEvent(event.id)
        }
    }

    fun syncWithGoogle(context: Context) {
        val event = _uiState.value.event ?: return
        if (event.googleEventId != null) return

        viewModelScope.launch {
            val calendarId = settingsManager.getGoogleCalendarId()
            if (calendarId.isEmpty() || calendarId == "primary") {
                return@launch
            }

            try {
                var eventToSync = event
                if (eventToSync.dtStart == null) {
                    val startLdt = LocalDate.parse(event.date).atTime(LocalTime.parse(event.startTime))
                    val endLdt = if (!event.endTime.isNullOrEmpty()) {
                        LocalDate.parse(event.date).atTime(LocalTime.parse(event.endTime!!))
                    } else {
                        startLdt.plusHours(1)
                    }
                    eventToSync = eventToSync.copy(
                        dtStart = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        dtEnd = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                }

                val gId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToSync, calendarId)
                if (gId != null) {
                    val updatedEvent = eventToSync.copy(googleEventId = gId.toString(), googleCalendarId = calendarId)
                    
                    // GOOGLE-FIRST: If private, delete from local (it's now in Google)
                    if (updatedEvent.sharedGroupIds.isEmpty()) {
                        dataManager.deleteCalendarEventLocalOnly(updatedEvent.id)
                    } else {
                        dataManager.saveCalendarEvent(updatedEvent)
                    }
                    
                    _uiState.update { it.copy(event = updatedEvent) }
                }
            } catch (e: Exception) {
                Log.e("CalendarDetailVM", "Manual Google Sync failed", e)
            }
        }
    }

    fun removeFromGoogle(context: Context) {
        val event = _uiState.value.event ?: return
        val gId = event.googleEventId ?: return

        viewModelScope.launch {
            try {
                GoogleCalendarSyncHelper.deleteFromGoogleCalendar(context, gId)
                val updatedEvent = event.copy(googleEventId = null, googleCalendarId = null)
                
                // Since it's no longer in Google, we MUST save it back to Firestore to avoid losing it
                dataManager.saveCalendarEvent(updatedEvent)
                
                _uiState.update { it.copy(event = updatedEvent) }
            } catch (e: Exception) {
                Log.e("CalendarDetailVM", "Remove from Google failed", e)
            }
        }
    }
}
