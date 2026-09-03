package com.jtool.android.features.calendar.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarEventDetailViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarEventDetailUiState())
    val uiState: StateFlow<CalendarEventDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadEvent()
    }

    private fun loadEvent() {
        val eventId = settingsManager.getSelectedCalendarEventId() ?: return
        val date = settingsManager.getSelectedCalendarDate()
        val userId = settingsManager.getUserId()
        val groupId = settingsManager.getCurrentGroupId()

        if (eventId.startsWith("virtual_")) {
            // Re-generate virtual event to find it
            val options = settingsManager.getWorktimeOptions()
            val day = LocalDate.parse(date)
            val virtuals = CalendarEventHelper.generateVirtualShifts(options, day, day)
            val event = virtuals.find { it.id == eventId }
            _uiState.update { it.copy(event = event) }
        } else {
            // Load from DB
            val privateFlow = dataManager.getPrivateCalendarEventsFlow(userId, date, date)
            val sharedFlow = if (groupId != null) {
                dataManager.getSharedCalendarEventsFlow(groupId, date, date)
            } else {
                flowOf(emptyList())
            }

            combine(privateFlow, sharedFlow) { private, shared ->
                private + shared
            }.onEach { events ->
                val event = events.find { it.id == eventId }
                _uiState.update { it.copy(event = event) }
            }.launchIn(viewModelScope)
        }
    }

    fun toggleEditDialog(show: Boolean) {
        _uiState.update { it.copy(showEditDialog = show) }
    }

    fun updateEvent(updatedEvent: CalendarEvent, isShared: Boolean) {
        viewModelScope.launch {
            if (isShared) {
                val groupId = settingsManager.getCurrentGroupId()
                if (groupId != null) {
                    dataManager.addSharedCalendarEvent(groupId, updatedEvent)
                }
            } else {
                dataManager.addPrivateCalendarEvent(settingsManager.getUserId(), updatedEvent)
            }
            _uiState.update { it.copy(event = updatedEvent, showEditDialog = false) }
        }
    }

    fun deleteEvent() {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            val isShared = event.groupsVisibility.isNotEmpty()
            if (isShared) {
                val groupId = settingsManager.getCurrentGroupId()
                if (groupId != null) {
                    dataManager.deleteSharedCalendarEvent(groupId, event.id)
                }
            } else {
                dataManager.deletePrivateCalendarEvent(settingsManager.getUserId(), event.id)
            }
        }
    }
}
