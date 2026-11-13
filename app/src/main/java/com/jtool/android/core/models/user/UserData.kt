package com.jtool.android.core.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.jtool.android.core.models.calendar.CalendarConfig
import com.jtool.android.core.models.workday.WorktimeOptions

@Keep
@IgnoreExtraProperties
data class UserData(
    val appUserId: String = "",
    val currentGroupId: String? = null,
    @get:PropertyName("isDarkMode")
    @PropertyName("isDarkMode")
    val isDarkMode: Boolean = true,
    val language: String = "en",
    val lastSupportDate: String = "",
    val dailySupportCount: Int = 0,
    val worktimeOptions: WorktimeOptions = WorktimeOptions(),
    val calendarConfig: CalendarConfig = CalendarConfig(),
    val lightPrimary: String = "0xFF059669",
    val lightBg: String = "0xFFF0FDF4",
    val darkPrimary: String = "0xFF10B981",
    val darkBg: String = "0xFF09090B",
    val seenVaultsInfo: Boolean = false,
    val jtoolsFolderId: String = "",
    val vaultsFolderId: String = "",
    val launcherConfig: LauncherConfig = LauncherConfig(),
    val notesConfig: NotesConfig = NotesConfig()
)
