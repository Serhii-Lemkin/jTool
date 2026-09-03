package com.jtool.android.core.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

import com.jtool.android.core.models.workday.WorktimeOptions
import com.jtool.android.core.models.calendar.CalendarEvent

@Keep
@IgnoreExtraProperties
data class UserData(
    val appUserId: String = "",
    val currentGroupId: String? = null,
    @get:PropertyName("isDarkMode")
    @PropertyName("isDarkMode")
    val isDarkMode: Boolean = true,
    val language: String = "en",
    val hours: Map<String, Double> = emptyMap(),
    val lastSupportDate: String = "",
    val dailySupportCount: Int = 0,
    val worktimeOptions: WorktimeOptions = WorktimeOptions(),
    val privateCalendarEvents: Map<String, CalendarEvent> = emptyMap()
)
