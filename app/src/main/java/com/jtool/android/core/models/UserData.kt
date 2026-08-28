package com.jtool.android.core.models

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
data class UserData(
    val appUserId: String = "",
    val currentGroupId: String? = null,
    @get:PropertyName("isDarkMode")
    @PropertyName("isDarkMode")
    val isDarkMode: Boolean = true,
    val language: String = "en",
    val hours: Map<String, Double> = emptyMap(),
    val lastSupportDate: String = "",
    val dailySupportCount: Int = 0
)
