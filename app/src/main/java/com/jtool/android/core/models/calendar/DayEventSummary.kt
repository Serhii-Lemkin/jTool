package com.jtool.android.core.models.calendar

import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum

data class DayEventSummary(
    val typeCounts: Map<CalendarEventTypeEnum, Int> = emptyMap()
)
