package com.jtool.android.features.admin

sealed interface AdminEvent {
    data class Navigate(val subScreen: String) : AdminEvent
    data class UpdateUserSearchQuery(val query: String) : AdminEvent
    object ApplyUserSearch : AdminEvent
    data class SetUserAdminStatus(val uid: String, val isAdmin: Boolean) : AdminEvent
    data class RequestDeleteUser(val uid: String?, val step: Int) : AdminEvent
    data class ConfirmDeleteUser(val uid: String) : AdminEvent
    data class UpdateGroupSearchQuery(val query: String) : AdminEvent
    object ApplyGroupSearch : AdminEvent
    data class RequestDeleteGroup(val gid: String?, val step: Int) : AdminEvent
    data class ConfirmDeleteGroup(val gid: String) : AdminEvent
    object Refresh : AdminEvent
    object StartLegacyFieldsMigration : AdminEvent
}
