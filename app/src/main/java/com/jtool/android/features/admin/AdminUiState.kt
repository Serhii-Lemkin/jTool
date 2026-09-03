package com.jtool.android.features.admin

import com.jtool.android.core.models.admin.enums.AdminSubScreenEnum

data class AdminUiState(
    val currentSubScreen: AdminSubScreenEnum = AdminSubScreenEnum.MAIN,
    val userNames: Map<String, String> = emptyMap(),
    val users: List<com.jtool.android.core.models.user.User> = emptyList(),
    val groups: List<com.jtool.android.core.models.group.GroupInfo> = emptyList(),
    val userSearchQuery: String = "",
    val appliedUserSearchQuery: String = "",
    val groupSearchQuery: String = "",
    val appliedGroupSearchQuery: String = "",
    val showUserDeleteConfirm1: String? = null,
    val showUserDeleteConfirm2: String? = null,
    val showGroupDeleteConfirm1: String? = null,
    val showGroupDeleteConfirm2: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
