package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class ChecklistItem(
    val id: String = "",
    val text: String = "",
    val isChecked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
