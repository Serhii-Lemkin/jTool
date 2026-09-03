package com.jtool.android.core.models.workday

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Keep
@IgnoreExtraProperties
@Serializable
data class WorkHistoryItem(
    val id: String = "",
    val date: String = "", // YYYY-MM-DD
    val startTime: String = "", // HH:mm
    val endTime: String? = null, // HH:mm, null if auto-finished
    val durationMillis: Long = 0,
    val scheduledDurationMillis: Long = 0,
    val mode: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
