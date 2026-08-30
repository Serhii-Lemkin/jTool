package com.jtool.android.features.settings

import com.jtool.android.core.models.common.enums.AppScreenEnum

data class SettingsUiState(
    val currentSubScreen: AppScreenEnum = AppScreenEnum.SETTINGS
)
