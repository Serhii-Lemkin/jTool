package com.jtool.android.features.settings.requests

sealed interface RequestsEvent {
    data class UpdateText(val text: String) : RequestsEvent
    object SendRequest : RequestsEvent
    object ResetStatus : RequestsEvent
}
