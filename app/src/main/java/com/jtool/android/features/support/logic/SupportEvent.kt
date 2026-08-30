package com.jtool.android.features.support.logic

import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportScreenTypeEnum
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum

sealed interface SupportEvent {
    data class SetFilterType(val type: SupportTypeEnum?) : SupportEvent
    data class ToggleArchived(val show: Boolean) : SupportEvent
    data class UpdateRequestText(val text: String) : SupportEvent
    data class SubmitRequest(val type: SupportTypeEnum, val context: android.content.Context) : SupportEvent
    data class OpenRequest(val request: SupportRequest) : SupportEvent
    data class UpdateStatus(val requestId: String, val status: SupportStatusEnum) : SupportEvent
    data class UpdateType(val requestId: String, val type: SupportTypeEnum) : SupportEvent
    data class AddComment(val requestId: String, val text: String, val parentId: String? = null) : SupportEvent
    data class EditComment(val requestId: String, val commentId: String, val newContent: String) : SupportEvent
    data class DeleteComment(val requestId: String, val commentId: String) : SupportEvent
    data class DeleteRequest(val requestId: String) : SupportEvent
    object ResetStatus : SupportEvent
    data class Navigate(val screen: SupportScreenTypeEnum) : SupportEvent
    
    object Refresh : SupportEvent
    object ToggleSearch : SupportEvent
    data class UpdateSearchQuery(val query: String) : SupportEvent
    object LoadMore : SupportEvent
}
