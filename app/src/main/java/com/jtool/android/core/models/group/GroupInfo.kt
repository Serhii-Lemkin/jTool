package com.jtool.android.core.models.group

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class GroupInfo(
    val id: String = "",
    val name: String = "",
    val ownerUid: String? = null,
    val createdAt: Long = 0L,
    @get:PropertyName("isPrivate")
    @PropertyName("isPrivate")
    val isPrivate: Boolean = false
)
