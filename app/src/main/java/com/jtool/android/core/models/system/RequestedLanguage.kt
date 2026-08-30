package com.jtool.android.core.models.system

import androidx.annotation.Keep

@Keep
data class RequestedLanguage(
    val name: String = "",
    val code: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val device: String = android.os.Build.MODEL
)
