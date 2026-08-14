package com.jtool.android.features.settings.visibility

data class AppVisibilityUiState(
    val isGridLayout: Boolean = false,
    val visibleApps: Map<String, Boolean> = emptyMap()
)
