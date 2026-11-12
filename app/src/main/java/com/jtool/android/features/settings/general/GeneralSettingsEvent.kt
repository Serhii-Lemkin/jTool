package com.jtool.android.features.settings.general

import java.time.DayOfWeek

sealed interface GeneralSettingsEvent {
    object ToggleTheme : GeneralSettingsEvent
    data class SetLanguage(val lang: String) : GeneralSettingsEvent
    data class SetThemeColors(val primaryHex: String, val bgHex: String, val isDarkMode: Boolean) : GeneralSettingsEvent
    data class SetWeeklyStartDay(val day: DayOfWeek) : GeneralSettingsEvent
    data class SetDateFormat(val format: String) : GeneralSettingsEvent
}
