package com.jtool.android.core.models.tracker

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Tracker(
    val id: String = "",
    val title: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val metrics: List<TrackerMetric> = emptyList(),
    val userColors: Map<String, Long> = emptyMap() // userId -> ARGB
)
