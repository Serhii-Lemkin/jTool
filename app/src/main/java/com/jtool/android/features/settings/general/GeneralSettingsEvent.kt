package com.jtool.android.features.settings.general

sealed interface GeneralSettingsEvent {
    object ToggleTheme : GeneralSettingsEvent
    object ToggleLanguage : GeneralSettingsEvent
}
