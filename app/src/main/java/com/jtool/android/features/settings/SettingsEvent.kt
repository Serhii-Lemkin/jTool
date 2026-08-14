package com.jtool.android.features.settings

sealed interface SettingsEvent {
    data class Navigate(val subScreen: String) : SettingsEvent
}
