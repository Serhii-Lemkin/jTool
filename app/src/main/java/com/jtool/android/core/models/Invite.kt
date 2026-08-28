package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class Invite(
    val inviteId: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val fromEmail: String = "",
    val timestamp: Long = 0L
)
