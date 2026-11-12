package com.jtool.android.features.links

import com.jtool.android.core.localization.domains.LinksLoc
import com.jtool.android.core.models.feature.links.LinkItem

data class LinksUiState(
    val links: List<LinkItem> = emptyList(),
    val searchQuery: String = "",
    val newTitle: String = "",
    val newUrl: String = "",
    val newTag: String = "#COMMON#",
    val selectedTags: Set<String> = emptySet(),
    val showAddDialog: Boolean = false,
    val editingLink: LinkItem? = null,
    val linkToDelete: LinkItem? = null,
    val menuExpanded: Boolean = false,
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val filteredLinks: List<LinkItem>
        get() {
            val allTagKeys = LinksLoc.allStandardLinkTags.map { it.first }.toSet()
            val applyTagFilter = selectedTags.isNotEmpty() && selectedTags.size < allTagKeys.size

            return links.filter { link ->
                val matchesQuery = searchQuery.isBlank() ||
                        link.title.contains(searchQuery, ignoreCase = true) ||
                        link.url.contains(searchQuery, ignoreCase = true)

                val effectiveTag = link.tag.ifBlank { "#COMMON#" }.uppercase()
                val matchesTag = !applyTagFilter || selectedTags.contains(effectiveTag)

                matchesQuery && matchesTag
            }.sortedByDescending { it.timestamp }
        }
}
