package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class NoteItem(
    val id: Long,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "Unknown"
)
