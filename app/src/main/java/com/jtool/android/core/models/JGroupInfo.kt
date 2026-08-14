package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class JGroupInfo(
    val id: String,
    val name: String,
    val owner: String? = null
)
