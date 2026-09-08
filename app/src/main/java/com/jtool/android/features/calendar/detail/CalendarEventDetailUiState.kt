package com.jtool.android.features.calendar.detail

import com.jtool.android.core.models.calendar.CalendarEvent

data class CalendarEventDetailUiState(
    val event: CalendarEvent? = null,
    val comments: List<com.jtool.android.core.models.common.Comment> = emptyList(),
    val isLoading: Boolean = false,
    val showEditDialog: Boolean = false
)
