package com.jtool.android.core.models.user

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class PublicProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String? = null,
    val calendarEmail: String? = null,
    val lastModified: Long = 0L
)
