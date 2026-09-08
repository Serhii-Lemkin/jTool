package com.jtool.android.core.models.tracker

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class TrackerMetric(
    val id: String = "",
    val name: String = "",
    val unit: String? = null,
    val color: Long? = null
)
