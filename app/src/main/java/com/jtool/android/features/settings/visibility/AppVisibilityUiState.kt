package com.jtool.android.features.settings.visibility

import com.jtool.android.core.models.common.enums.AppScreenEnum

data class AppVisibilityUiState(
    val isGridLayout: Boolean = false,
    val visibleApps: Map<AppScreenEnum, Boolean> = emptyMap()
)
