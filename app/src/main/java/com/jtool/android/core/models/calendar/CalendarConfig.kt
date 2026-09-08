package com.jtool.android.core.models.calendar

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Keep
@IgnoreExtraProperties
@Serializable
data class CalendarConfig(
    @get:PropertyName("googleSyncEnabled")
    @PropertyName("googleSyncEnabled")
    val googleSyncEnabled: Boolean = false,
    
    @get:PropertyName("googlePullEnabled")
    @PropertyName("googlePullEnabled")
    val googlePullEnabled: Boolean = true,
    
    @get:PropertyName("googlePushEnabled")
    @PropertyName("googlePushEnabled")
    val googlePushEnabled: Boolean = false,
    
    val googleCalendarId: String = "primary",
    val autoWorkKeywords: String = "Work,Shift,Workday",
    val lastMigrationTimestamp: Long = 0L,
    val lastGoogleSyncTimestamp: Long = 0L
)
