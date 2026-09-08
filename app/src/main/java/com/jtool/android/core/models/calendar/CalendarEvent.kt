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
    val date: String = "", // Legacy: keeping for backward compatibility
    val startTime: String = "", // Legacy: keeping for backward compatibility
    val endTime: String? = null, // Legacy: keeping for backward compatibility
    val dateCreated: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val sharedGroupIds: List<String> = emptyList(), // Visibility list
    val note: String = "",
    val status: CalendarEventStatusEnum = CalendarEventStatusEnum.REGULAR,
    val origin: String? = null,
    val allDay: Boolean = false,

    // Collaboration & Permissions
    val ownerUid: String = "",
    val originalEventId: String? = null,
    val sharedToGroups: Map<String, String> = emptyMap(),

    // Google Sync
    val googleCalendarId: String? = null,
    val googleEventId: String? = null,
    val dtStart: Long? = null, // SOURCE OF TRUTH
    val dtEnd: Long? = null  // SOURCE OF TRUTH
)
