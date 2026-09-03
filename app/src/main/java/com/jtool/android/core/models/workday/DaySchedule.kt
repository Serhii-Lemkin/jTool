package com.jtool.android.core.models.workday

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

import com.google.firebase.firestore.PropertyName

@Keep
@Serializable
data class DaySchedule(
    val shifts: List<WorkShift> = emptyList(),
    @get:PropertyName("isConfirmedOff")
    @PropertyName("isConfirmedOff")
    val isConfirmedOff: Boolean = false,
    
    // Legacy support for backward compatibility during migration
    val start: String = "",
    val end: String = ""
)
