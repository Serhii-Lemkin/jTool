package com.jtool.android.features.calendar.day

import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import java.time.LocalDate

enum class CalendarDayViewMode {
    TIMELINE,
    LIST
}

data class CalendarDayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val privateEvents: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = false,
    val showAddEditDialog: Boolean = false,
    val editingEvent: CalendarEvent? = null,
    val extensions: Map<String, CalendarEventExtension> = emptyMap(),
    val autoOpenAdd: Boolean = false,
    val viewMode: CalendarDayViewMode = CalendarDayViewMode.TIMELINE,
    val showDeclinedEvents: Boolean = false
)
