package com.jtool.android.features.settings.groups

sealed interface GroupsEvent {
    data class SwitchGroup(val groupId: String) : GroupsEvent
    data class CreateGroup(val name: String) : GroupsEvent
    data class JoinGroup(val id: String) : GroupsEvent
    data class LeaveGroup(val id: String) : GroupsEvent
    data class DeleteGroup(val id: String) : GroupsEvent
    data class UpdateGroupName(val id: String, val newName: String) : GroupsEvent
    data class SendInvite(val email: String, val groupId: String) : GroupsEvent
    object DismissCreatedDialog : GroupsEvent
    object DismissMessage : GroupsEvent
    object DismissJoinRequestSentSheet : GroupsEvent
    object Refresh : GroupsEvent
}
