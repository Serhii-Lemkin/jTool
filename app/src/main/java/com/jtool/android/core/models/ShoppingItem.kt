package com.jtool.android.core.models

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class ShoppingItem(
    val id: String = "",
    val text: String = "",
    @get:PropertyName("isChecked")
    @PropertyName("isChecked")
    val isChecked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "",
    val aisle: String = "Common"
)
