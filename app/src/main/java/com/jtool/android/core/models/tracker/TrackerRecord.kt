package com.jtool.android.core.models.tracker

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class TrackerRecord(
    val id: String = "",
    val trackerId: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Long = 0L,
    val values: Map<String, Double> = emptyMap() // metricId -> value
)
