package com.jtool.android.features.settings

import com.jtool.android.core.models.common.enums.AppScreenEnum

sealed interface SettingsEvent {
    data class Navigate(val subScreen: AppScreenEnum) : SettingsEvent
}
