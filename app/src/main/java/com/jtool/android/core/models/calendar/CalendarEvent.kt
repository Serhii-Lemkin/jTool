package com.jtool.android.core.models.calendar

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jtool.android.core.models.calendar.enums.CalendarEventStatusEnum
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import kotlinx.serialization.Serializable

@Keep
@IgnoreExtraProperties
@Serializable
data class CalendarEvent(
    val id: String = "",
    val title: String = "",
    val type: CalendarEventTypeEnum = CalendarEventTypeEnum.COMMON,
    val date: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val groupsVisibility: List<String> = emptyList(),
    val note: String = "",
    val status: CalendarEventStatusEnum = CalendarEventStatusEnum.REGULAR
)
