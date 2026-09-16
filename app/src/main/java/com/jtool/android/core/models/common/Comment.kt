package com.jtool.android.core.models.common

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@IgnoreExtraProperties
data class Comment(
    var id: String = "",
    val content: String = "",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isEdited")
    val isEdited: Boolean = false,
    val parentId: String? = null,
    @get:PropertyName("isPending")
    val isPending: Boolean = false,
    val replies: List<Comment> = emptyList()
)
