package com.jtool.android.core.models.group

import androidx.annotation.Keep

@Keep
data class GroupInfo(
    val id: String = "",
    val name: String = "",
    val ownerUid: String? = null,
    val createdAt: Long = 0L
)
