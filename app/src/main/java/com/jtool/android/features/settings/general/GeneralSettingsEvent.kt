package com.jtool.android.features.settings.general

sealed interface GeneralSettingsEvent {
    object ToggleTheme : GeneralSettingsEvent
    data class SetLanguage(val lang: String) : GeneralSettingsEvent
    data class SetThemeColors(val primaryHex: String, val bgHex: String, val isDarkMode: Boolean) : GeneralSettingsEvent
}
