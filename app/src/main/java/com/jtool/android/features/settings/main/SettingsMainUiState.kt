package com.jtool.android.features.settings.main

data class SettingsMainUiState(
    val userEmail: String? = null,
    val currentUser: com.google.firebase.auth.FirebaseUser? = null
)
