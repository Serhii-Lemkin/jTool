package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class GroupInfo(
    val id: String,
    val name: String,
    val owner: String? = null
)
