package com.jtool.android.core.models.group

import androidx.annotation.Keep

@Keep
data class JoinRequest(
    val groupId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val timestamp: Long = 0L
)
