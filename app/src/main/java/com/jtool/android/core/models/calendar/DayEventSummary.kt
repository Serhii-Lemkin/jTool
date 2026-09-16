package com.jtool.android.core.models.calendar

import androidx.annotation.Keep

@Keep
data class EventPreview(
    val id: String = "",
    val title: String = "",
    val colorKey: String? = null,
    val isDeclined: Boolean = false
)

@Keep
data class DayEventSummary(
    val hasEvents: Boolean = false,
    val eventCount: Int = 0,
    val eventColorKeys: List<String?> = emptyList(),
    val eventPreviews: List<EventPreview> = emptyList()
)
