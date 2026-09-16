package com.jtool.android.features.calendar.detail

import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.common.Comment

data class CalendarEventDetailUiState(
    val event: CalendarEvent? = null,
    val extension: CalendarEventExtension? = null,
    val userNames: Map<String, String> = emptyMap(),
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isResponding: Boolean = false,
    val isPermissionDenied: Boolean = false,
    val showEditDialog: Boolean = false,
    val isSyncEnabled: Boolean = true
)
