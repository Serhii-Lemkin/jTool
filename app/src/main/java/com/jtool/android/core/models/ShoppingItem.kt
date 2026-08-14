package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class ShoppingItem(
    val id: Long,
    val text: String,
    val isChecked: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "Unknown",
    val aisle: String = "Common"
)
