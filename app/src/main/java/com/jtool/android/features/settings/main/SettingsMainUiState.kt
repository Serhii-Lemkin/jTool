package com.jtool.android.features.settings.main

data class SettingsMainUiState(
    val hasUpdate: Boolean = false,
    val isSyncMissing: Boolean = false,
    val userEmail: String? = null,
    val currentUser: com.google.firebase.auth.FirebaseUser? = null
)
