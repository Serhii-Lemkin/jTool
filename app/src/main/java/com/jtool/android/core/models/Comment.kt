package com.jtool.android.core.models

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class Comment(
    val id: String = "",
    val content: String = "",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isEdited")
    @PropertyName("isEdited")
    val isEdited: Boolean = false,
    val parentId: String? = null,
    @get:PropertyName("isPending")
    @PropertyName("isPending")
    val isPending: Boolean = false,
    val replies: List<Comment> = emptyList()
)
