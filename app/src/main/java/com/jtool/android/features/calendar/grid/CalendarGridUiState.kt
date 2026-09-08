package com.jtool.android.features.calendar.grid

import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.DayEventSummary
import java.time.LocalDate
import java.time.YearMonth

data class CalendarGridUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val eventSummaries: Map<String, DayEventSummary> = emptyMap(),
    val privateEvents: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = false,
    val showAddEditDialog: Boolean = false,
    val editingEvent: CalendarEvent? = null,
    val syncResultMessage: String? = null,
    val visibleGroupIds: Set<String> = emptySet()
)
