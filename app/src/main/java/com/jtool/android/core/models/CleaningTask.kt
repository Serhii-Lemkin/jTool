package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class CleaningTask(
    val id: Long,
    val title: String,
    val frequencyDays: Int,
    val isRecurring: Boolean,
    val lastDoneTimestamp: Long = 0L,
    val isChecked: Boolean = false,
    val isEnabled: Boolean = true,
    val lastUpdatedBy: String = "Unknown",
    val timestamp: Long = System.currentTimeMillis()
)
