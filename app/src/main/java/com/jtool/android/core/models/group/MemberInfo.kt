package com.jtool.android.core.models.group

import androidx.annotation.Keep

@Keep
data class MemberInfo(
    val joinedAt: Long = 0L,
    val calendarEmail: String? = null
)
