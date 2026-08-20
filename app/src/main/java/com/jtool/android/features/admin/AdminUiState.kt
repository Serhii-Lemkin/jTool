package com.jtool.android.features.admin

data class AdminUiState(
    val currentSubScreen: String = "main",
    val userNames: Map<String, String> = emptyMap(),
    val users: List<Map<String, Any?>> = emptyList(),
    val groups: List<Map<String, Any?>> = emptyList(),
    val userSearchQuery: String = "",
    val appliedUserSearchQuery: String = "",
    val groupSearchQuery: String = "",
    val appliedGroupSearchQuery: String = "",
    val showUserDeleteConfirm1: String? = null,
    val showUserDeleteConfirm2: String? = null,
    val showGroupDeleteConfirm1: String? = null,
    val showGroupDeleteConfirm2: String? = null,
    val migrationStatus: String? = null,
    val isMigrating: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
