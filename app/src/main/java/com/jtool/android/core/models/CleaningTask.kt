package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class CleaningTask(
    val id: String = "",
    val title: String = "",
    val frequencyDays: Int = 1,
    val isRecurring: Boolean = true,
    val lastDoneTimestamp: Long = 0L,
    val isChecked: Boolean = false,
    val isEnabled: Boolean = true,
    val lastUpdatedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
