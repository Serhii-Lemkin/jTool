package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class ShoppingItem(
    val id: String = "",
    val text: String = "",
    val isChecked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "",
    val aisle: String = "Common"
)
