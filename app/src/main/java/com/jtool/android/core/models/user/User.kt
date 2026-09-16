package com.jtool.android.core.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
data class User(
    var uid: String = "",
    val email: String? = null,
    val groupIds: List<String> = emptyList(),
    val lastModified: Long = 0L,
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false
)
