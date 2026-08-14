package com.jtool.android.features.settings.account

sealed interface AccountEvent {
    data class UpdateUserName(val name: String) : AccountEvent
    object SignOut : AccountEvent
    object DeleteAccount : AccountEvent
}
