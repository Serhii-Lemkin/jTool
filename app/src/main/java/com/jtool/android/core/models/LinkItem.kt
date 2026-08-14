package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
data class LinkItem(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = ""
)
