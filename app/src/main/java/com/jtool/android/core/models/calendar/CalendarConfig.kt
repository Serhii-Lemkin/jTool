package com.jtool.android.core.models.calendar

import androidx.annotation.Keep
import com.jtool.android.core.models.calendar.enums.GridEventStyleEnum
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CalendarConfig(
    val googleCalendarId: String = "",
    val googleCalendarEmail: String = "",
    val googleCalendarSharedId: String = "",
    val workShiftColorKey: String = "7",
    val displayedCalendarIds: List<String> = emptyList(),
    val gridEventStyle: GridEventStyleEnum = GridEventStyleEnum.DOTS
)
