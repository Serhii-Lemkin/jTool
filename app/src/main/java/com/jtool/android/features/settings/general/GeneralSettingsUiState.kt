package com.jtool.android.features.settings.general

import java.time.DayOfWeek

data class GeneralSettingsUiState(
    val isDarkMode: Boolean = true,
    val language: String = "en",
    val lightPrimary: String = "0xFF059669",
    val lightBg: String = "0xFFF0FDF4",
    val darkPrimary: String = "0xFF10B981",
    val darkBg: String = "0xFF09090B",
    val weeklyStartDay: DayOfWeek = DayOfWeek.MONDAY
)
