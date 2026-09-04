package com.jtool.android.features.calendar.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarDayViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarDayUiState())
    val uiState: StateFlow<CalendarDayUiState> = _uiState.asStateFlow()

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

    private fun loadEvents(date: LocalDate) {
        val userId = settingsManager.getUserId()
        val groupId = settingsManager.getCurrentGroupId()
        val options = settingsManager.getWorktimeOptions()
        val dateStr = date.toString()
        val prevDateStr = date.minusDays(1).toString()

        val privateFlow = dataManager.getPrivateCalendarEventsFlow(userId, prevDateStr, dateStr)
        val sharedFlow = if (groupId != null) {
            dataManager.getSharedCalendarEventsFlow(groupId, prevDateStr, dateStr)
        } else {
            flowOf(emptyList())
        }

        // Fetch today and yesterday's events to find overlaps
        combine(privateFlow, sharedFlow) { private, shared ->
            private + shared
        }.onEach { allEvents ->
            val virtualEvents = if (options.trackingPreference == com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum.LOG_HOURS) {
                CalendarEventHelper.generateVirtualShifts(options, date, date)
            } else emptyList()
            
            val displayEvents = mutableListOf<CalendarEvent>()
            
            // Process real events
            allEvents.forEach { event ->
                val eventDate = LocalDate.parse(event.date)
                val startTime = CalendarEventHelper.parseTime(event.startTime)
                val endTime = if (event.endTime.isNullOrBlank()) startTime.plusHours(1) else CalendarEventHelper.parseTime(event.endTime)
                val isOvernight = endTime.isBefore(startTime) || (endTime == startTime && !event.endTime.isNullOrBlank())

                if (eventDate == date) {
                    // Starts today
                    if (isOvernight) {
                        // Part 1: starts today, ends tomorrow
                        displayEvents.add(event.copy(note = "Ends tomorrow. ${event.note}".trim()))
                    } else {
                        displayEvents.add(event)
                    }
                } else if (eventDate == date.minusDays(1) && isOvernight) {
                    // Started yesterday and it's overnight, so it ends today
                    displayEvents.add(event.copy(
                        date = date.toString(),
                        startTime = "00:00",
                        note = "Continued from yesterday. ${event.note}".trim(),
                        id = "${event.id}_cont" // Unique ID for display
                    ))
                }
            }

            // Merge virtual shifts if today has no real shift
            val hasRealShift = displayEvents.any { it.type == CalendarEventTypeEnum.SHIFT }
            if (!hasRealShift) {
                displayEvents.addAll(virtualEvents)
            }

            _uiState.update { it.copy(privateEvents = displayEvents.sortedBy { it.startTime }) }
        }.launchIn(viewModelScope)
    }

    fun toggleAddEditDialog(show: Boolean, event: CalendarEvent? = null) {
        if (event != null) {
            settingsManager.setSelectedCalendarEventId(event.id)
        }
        _uiState.update { it.copy(showAddEditDialog = show, editingEvent = event) }
    }

    fun addEvent(event: CalendarEvent, isShared: Boolean) {
        viewModelScope.launch {
            if (isShared) {
                val groupId = settingsManager.getCurrentGroupId()
                if (groupId != null) {
                    dataManager.addSharedCalendarEvent(groupId, event.copy(createdBy = settingsManager.getUserId()))
                }
            } else {
                dataManager.addPrivateCalendarEvent(settingsManager.getUserId(), event.copy(createdBy = settingsManager.getUserId()))
            }
        }
    }

    fun deleteEvent(eventId: String, isShared: Boolean) {
        viewModelScope.launch {
            if (isShared) {
                val groupId = settingsManager.getCurrentGroupId()
                if (groupId != null) {
                    dataManager.deleteSharedCalendarEvent(groupId, eventId)
                }
            } else {
                dataManager.deletePrivateCalendarEvent(settingsManager.getUserId(), eventId)
            }
        }
    }
}
