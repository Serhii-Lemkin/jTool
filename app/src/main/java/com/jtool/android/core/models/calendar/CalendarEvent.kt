package com.jtool.android.core.models.calendar

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Keep
@Serializable
data class CalendarEvent(
    val id: String = "", // Google Event ID
    val title: String = "",
    val note: String = "",
    val allDay: Boolean = false,
    val dtStart: Long? = null,
    val dtEnd: Long? = null,
    val attendees: List<String> = emptyList(), // Emails
    val googleCalendarId: String? = null,
    val currentUserStatus: Int? = null, // From CalendarContract.Attendees.ATTENDEE_STATUS
    val iCalUID: String? = null,
    val organizer: String? = null,
    val attendeeStatuses: Map<String, Int> = emptyMap(),
    val colorKey: String? = null
) {
    val date: String get() = dtStart?.let { 
        if (allDay) {
            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
        } else {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() 
        }
    } ?: ""

    val startTime: String get() = dtStart?.let { 
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    } ?: ""

    val endTime: String get() = dtEnd?.let { 
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    } ?: ""
}
