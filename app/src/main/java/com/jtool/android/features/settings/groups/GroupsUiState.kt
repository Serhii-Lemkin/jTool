package com.jtool.android.features.settings.groups

import com.jtool.android.core.models.GroupInfo

data class GroupsUiState(
    val groups: Map<String, GroupInfo> = emptyMap(),
    val currentGroupId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLastMember: Boolean = false,
    val lastCreatedGroupId: String? = null
)
