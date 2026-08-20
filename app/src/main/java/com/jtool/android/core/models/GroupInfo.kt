package com.jtool.android.core.models

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class GroupInfo(
    val id: String = "",
    val name: String = "",
    @get:PropertyName("ownerUid")
    val owner: String? = null
)
