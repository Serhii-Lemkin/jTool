package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class Comment(
    val id: String = "",
    val content: String = "",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val parentId: String? = null,
    val isPending: Boolean = false,
    val replies: List<Comment> = emptyList()
)
