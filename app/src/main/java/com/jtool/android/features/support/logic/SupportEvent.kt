package com.jtool.android.features.support.logic

sealed interface SupportEvent {
    data class SetFilterType(val type: SupportType?) : SupportEvent
    data class ToggleArchived(val show: Boolean) : SupportEvent
    data class UpdateRequestText(val text: String) : SupportEvent
    data class SubmitRequest(val type: SupportType, val context: android.content.Context) : SupportEvent
    data class OpenRequest(val request: SupportRequest) : SupportEvent
    data class UpdateStatus(val requestId: String, val status: SupportStatus) : SupportEvent
    data class UpdateType(val requestId: String, val type: SupportType) : SupportEvent
    data class AddComment(val requestId: String, val text: String, val parentId: String? = null) : SupportEvent
    data class EditComment(val requestId: String, val commentId: String, val newContent: String) : SupportEvent
    data class DeleteComment(val requestId: String, val commentId: String) : SupportEvent
    data class DeleteRequest(val requestId: String) : SupportEvent
    object ResetStatus : SupportEvent
    data class Navigate(val screen: SupportScreenType) : SupportEvent
    
    object Refresh : SupportEvent
    object ToggleSearch : SupportEvent
    data class UpdateSearchQuery(val query: String) : SupportEvent
    object LoadMore : SupportEvent
}
