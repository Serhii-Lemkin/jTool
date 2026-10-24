package com.jtool.android.core.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Keep
@IgnoreExtraProperties
@Serializable
data class LauncherConfig(
    val appOrder: List<String> = emptyList(),
    val hiddenApps: List<String> = emptyList(),
    val gridColumnCount: Int = 3,
    val gestures: Map<String, String> = emptyMap()
)
