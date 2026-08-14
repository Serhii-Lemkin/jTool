package com.jtool.android.features.settings.updates

data class AppUpdatesUiState(
    val currentVersionCode: Int = 0,
    val currentVersionName: String = "",
    val availableVersionCode: Int? = null,
    val availableVersionName: String? = null,
    val isChecking: Boolean = false,
    val updateUrl: String? = null
)
