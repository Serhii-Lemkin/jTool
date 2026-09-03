package com.jtool.android.features.calendar.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.core.models.calendar.DayEventSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarGridUiState())
    val uiState: StateFlow<CalendarGridUiState> = _uiState.asStateFlow()

    private var eventsJob: Job? = null

    init {
        loadEvents()
    }

    private fun loadEvents() {
        eventsJob?.cancel()
        val userId = settingsManager.getUserId()
        val groupId = settingsManager.getCurrentGroupId()
        val options = settingsManager.getWorktimeOptions()

        // Wide range to cover month view and surrounding days for summary
        val currentMonth = _uiState.value.selectedMonth
        val start = currentMonth.atDay(1).minusWeeks(1)
        val end = currentMonth.atEndOfMonth().plusWeeks(1)

        val privateFlow = dataManager.getPrivateCalendarEventsFlow(userId, start.toString(), end.toString())
        val sharedFlow = if (groupId != null) {
            dataManager.getSharedCalendarEventsFlow(groupId, start.toString(), end.toString())
        } else {
            flowOf(emptyList())
        }

        eventsJob = combine(privateFlow, sharedFlow) { private, shared ->
            private + shared
        }.onEach { allRealEvents ->
            val virtualEvents = if (options.trackingPreference == com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum.LOG_HOURS) {
                CalendarEventHelper.generateVirtualShifts(options, start, end)
            } else emptyList()
            
            val mergedEvents = allRealEvents.toMutableList()
            val realShiftDates = allRealEvents.filter { it.type == CalendarEventTypeEnum.SHIFT }.map { it.date }.toSet()
            
            virtualEvents.forEach { virtual ->
                if (!realShiftDates.contains(virtual.date)) {
                    mergedEvents.add(virtual)
                }
            }

            val summaries = mergedEvents.groupBy { it.date }.mapValues { (_, dayEvents) ->
                DayEventSummary(
                    typeCounts = dayEvents.groupBy { it.type }.mapValues { it.value.size }
                )
            }
            
            _uiState.update { it.copy(
                privateEvents = mergedEvents, 
                eventSummaries = summaries,
                isLoading = false
            ) }
        }.launchIn(viewModelScope)
    }

    fun onDateSelected(date: LocalDate) {
        settingsManager.setSelectedCalendarDate(date.toString())
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun onMonthChange(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month, isLoading = true) }
        loadEvents()
    }

    fun toggleAddEditDialog(show: Boolean, event: CalendarEvent? = null) {
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

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadEvents()
    }

    fun triggerAutoAddOnDayView() {
        settingsManager.setAutoOpenCalendarAdd(true)
    }
}
