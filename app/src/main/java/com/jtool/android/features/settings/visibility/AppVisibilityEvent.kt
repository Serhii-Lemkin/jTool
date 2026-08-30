package com.jtool.android.features.settings.visibility

import com.jtool.android.core.models.common.enums.AppScreenEnum

sealed interface AppVisibilityEvent {
    data class ToggleAppVisibility(val appId: AppScreenEnum, val visible: Boolean) : AppVisibilityEvent
    object ToggleGridLayout : AppVisibilityEvent
}
