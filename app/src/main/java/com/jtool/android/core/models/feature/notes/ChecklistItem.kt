package com.jtool.android.core.models.feature.notes

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
data class ChecklistItem(
    var id: String = "",
    val text: String = "",
    @get:PropertyName("isChecked")
    val isChecked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
