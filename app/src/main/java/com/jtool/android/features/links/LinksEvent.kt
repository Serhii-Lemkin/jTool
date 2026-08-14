package com.jtool.android.features.links

import com.jtool.android.core.models.LinkItem

sealed interface LinksEvent {
    data class UpdateSearchQuery(val query: String) : LinksEvent
    data class UpdateNewTitle(val title: String) : LinksEvent
    data class UpdateNewUrl(val url: String) : LinksEvent
    data class ToggleAddDialog(val show: Boolean) : LinksEvent
    data class RequestDelete(val link: LinkItem?) : LinksEvent
    object ConfirmDelete : LinksEvent
    data class ToggleMenu(val expanded: Boolean) : LinksEvent
    object AddLink : LinksEvent
}
