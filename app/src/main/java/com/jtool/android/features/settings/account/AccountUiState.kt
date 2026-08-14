package com.jtool.android.features.settings.account

data class AccountUiState(
    val currentUserEmail: String? = null,
    val userName: String = "",
    val isLoading: Boolean = false
)
