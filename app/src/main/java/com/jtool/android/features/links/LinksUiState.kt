package com.jtool.android.features.links

import com.jtool.android.core.models.feature.links.LinkItem

data class LinksUiState(
    val links: List<LinkItem> = emptyList(),
    val searchQuery: String = "",
    val newTitle: String = "",
    val newUrl: String = "",
    val showAddDialog: Boolean = false,
    val linkToDelete: LinkItem? = null,
    val menuExpanded: Boolean = false,
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredLinks: List<LinkItem>
        get() = if (searchQuery.isBlank()) {
            links.sortedByDescending { it.timestamp }
        } else {
            links.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.url.contains(searchQuery, ignoreCase = true) 
            }.sortedByDescending { it.timestamp }
        }
}
