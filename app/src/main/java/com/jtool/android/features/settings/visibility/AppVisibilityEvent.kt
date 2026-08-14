package com.jtool.android.features.settings.visibility

sealed interface AppVisibilityEvent {
    data class ToggleAppVisibility(val appId: String, val visible: Boolean) : AppVisibilityEvent
    object ToggleGridLayout : AppVisibilityEvent
}
