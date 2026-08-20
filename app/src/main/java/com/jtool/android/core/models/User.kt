package com.jtool.android.core.models

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val email: String? = null,
    val groupIds: List<String> = emptyList(),
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false
)
