package com.jtool.android.features.settings.updates

sealed interface AppUpdatesEvent {
    object CheckUpdates : AppUpdatesEvent
}
