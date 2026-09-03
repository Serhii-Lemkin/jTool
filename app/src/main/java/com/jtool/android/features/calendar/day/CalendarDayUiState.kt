package com.jtool.android.features.calendar.day

import com.jtool.android.core.models.calendar.CalendarEvent
import java.time.LocalDate

data class CalendarDayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val privateEvents: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = false,
    val showAddEditDialog: Boolean = false,
    val editingEvent: CalendarEvent? = null,
    val autoOpenAdd: Boolean = false
)
