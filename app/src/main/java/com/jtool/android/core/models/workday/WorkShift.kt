package com.jtool.android.core.models.workday

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class WorkShift(
    val start: String? = null,
    val end: String? = null
)
