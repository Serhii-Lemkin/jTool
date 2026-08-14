package com.jtool.android.features.settings.requests

data class RequestsUiState(
    val requestText: String = "",
    val isSending: Boolean = false,
    val isSuccess: Boolean = false
)
