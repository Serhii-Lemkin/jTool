package com.jtool.android.features.calendar.detail

import com.jtool.android.core.models.calendar.CalendarEvent

data class CalendarEventDetailUiState(
    val event: CalendarEvent? = null,
    val isLoading: Boolean = false,
    val showEditDialog: Boolean = false
)
