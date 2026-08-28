package com.jtool.android.core.models

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class CleaningTask(
    val id: String = "",
    val title: String = "",
    val frequencyDays: Int = 1,
    @get:PropertyName("isRecurring")
    @PropertyName("isRecurring")
    val isRecurring: Boolean = true,
    val lastDoneTimestamp: Long = 0L,
    @get:PropertyName("isChecked")
    @PropertyName("isChecked")
    val isChecked: Boolean = false,
    @get:PropertyName("isEnabled")
    @PropertyName("isEnabled")
    val isEnabled: Boolean = true,
    val lastUpdatedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
