package com.jtool.android.core.models.feature.cleaning

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
data class CleaningTask(
    var id: String = "",
    val title: String = "",
    val frequencyDays: Int = 1,
    @get:PropertyName("isRecurring")
    val isRecurring: Boolean = true,
    val lastDoneTimestamp: Long = 0L,
    @get:PropertyName("isChecked")
    val isChecked: Boolean = false,
    @get:PropertyName("isEnabled")
    val isEnabled: Boolean = true,
    val lastUpdatedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
