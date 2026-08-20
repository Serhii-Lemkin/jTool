package com.jtool.android.features.support.logic

data class SupportUiState(
    val requests: List<SupportRequest> = emptyList(),
    val isLoading: Boolean = false,
    val isAdmin: Boolean = false,
    val showArchived: Boolean = false,
    val filterType: SupportType? = null, // null means "All"
    
    val isSending: Boolean = false,
    val isSuccess: Boolean = false,
    val requestText: String = "",
    val dailyCount: Int = 0,
    val maxRequests: Int = 2,
    val privacyUrl: String = "",
    val error: String? = null,
    
    val selectedRequest: SupportRequest? = null,
    val currentSubScreen: SupportScreenType = SupportScreenType.HUB,
    
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val pagingLimit: Int = 15
)

enum class SupportScreenType {
    HUB, DETAIL, NEW_FORM
}
