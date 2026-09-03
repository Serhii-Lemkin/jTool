package com.jtool.android.core.models.workday

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

import com.google.firebase.firestore.PropertyName

@Keep
@Serializable
data class DayLogHours(
    val hours: Int = 8,
    val minutes: Int = 0,
    @get:PropertyName("isConfirmedOff")
    @PropertyName("isConfirmedOff")
    val isConfirmedOff: Boolean = false,
    val approximateStartTime: String? = null,
    val approximateEndTime: String? = null
)
