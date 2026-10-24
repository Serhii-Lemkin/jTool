package com.jtool.android.features.support.logic

import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportScreenTypeEnum
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum

data class SupportUiState(
    val requests: List<SupportRequest> = emptyList(),
    val isLoading: Boolean = false,
    val isAdmin: Boolean = false,
    val isReviewer: Boolean = false,
    val activeTypeFilters: Set<SupportTypeEnum> = emptySet(),
    val activeStatusFilters: Set<SupportStatusEnum> = setOf(SupportStatusEnum.OPENED, SupportStatusEnum.IN_PROGRESS),
    val newRequestType: SupportTypeEnum = SupportTypeEnum.BUG,
    
    val isSending: Boolean = false,
    val isSuccess: Boolean = false,
    val requestText: String = "",
    val dailyCount: Int = 0,
    val maxRequests: Int = 2,
    val privacyUrl: String = "",
    val error: String? = null,
    
    val selectedRequest: SupportRequest? = null,
    val currentSubScreen: SupportScreenTypeEnum = SupportScreenTypeEnum.HUB,
    
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val pagingLimit: Int = 15
) {
    val canSeeAll: Boolean get() = isAdmin || isReviewer
    val canManageRequests: Boolean get() = isAdmin || isReviewer
}
