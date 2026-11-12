package com.jtool.android.features.links

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.jtool.android.core.models.feature.links.LinkItem
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.BaseSyncViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LinksViewModel(
    private val manager: LinksManager,
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : BaseSyncViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(LinksUiState())
    val uiState: StateFlow<LinksUiState> = _uiState.asStateFlow()

    init {
        observeGroupId()
        observeSyncData()
        
        isLoading.onEach { loading -> _uiState.update { it.copy(isLoading = loading) } }.launchIn(viewModelScope)
        isRefreshing.onEach { refreshing -> _uiState.update { it.copy(isRefreshing = refreshing) } }.launchIn(viewModelScope)
    }

    private fun observeGroupId() {
        dataManager.currentGroupIdFlow()
            .onEach { gid ->
                _uiState.update { it.copy(links = emptyList()) }
                clearListeners()
                val localLinks = manager.load(gid)
                _uiState.update { it.copy(links = localLinks) }
                if (gid != null) {
                    observeFirestoreLinks(gid)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getLinksColl(gid: String) = firestore.collection("groups").document(gid).collection("links")

    private fun observeFirestoreLinks(gid: String) {
        val listener = getLinksColl(gid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                _uiState.update { it.copy(isLoading = true) }
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(LinkItem::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        Log.e("LinksVM", "Error deserializing link ${doc.id}", e)
                        null
                    }
                }
                _uiState.update { it.copy(links = list, isLoading = false) }
                manager.save(list, gid)
            }
        }
        addListener(listener)
    }

    private fun observeSyncData() {
        dataManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: LinksEvent) {
        when (event) {
            is LinksEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.query) }
            is LinksEvent.UpdateNewTitle -> _uiState.update { it.copy(newTitle = event.title) }
            is LinksEvent.UpdateNewUrl -> _uiState.update { it.copy(newUrl = event.url) }
            is LinksEvent.UpdateNewTag -> _uiState.update { it.copy(newTag = event.tag) }
            is LinksEvent.ToggleTagFilter -> {
                val current = _uiState.value.selectedTags
                val updated = if (current.contains(event.tag)) current - event.tag else current + event.tag
                _uiState.update { it.copy(selectedTags = updated) }
            }
            is LinksEvent.SetTagFilters -> _uiState.update { it.copy(selectedTags = event.tags) }
            LinksEvent.ClearTagFilters -> _uiState.update { it.copy(selectedTags = emptySet()) }
            is LinksEvent.ToggleAddDialog -> {
                _uiState.update { 
                    it.copy(
                        showAddDialog = event.show, 
                        editingLink = if (!event.show) null else it.editingLink,
                        newTitle = if (!event.show) "" else it.newTitle,
                        newUrl = if (!event.show) "" else it.newUrl,
                        newTag = if (!event.show) "#COMMON#" else it.newTag
                    ) 
                }
            }
            is LinksEvent.RequestEdit -> {
                val link = event.link
                if (link != null) {
                    val tagVal = link.tag.ifBlank { "#COMMON#" }
                    _uiState.update { it.copy(editingLink = link, newTitle = link.title, newUrl = link.url, newTag = tagVal, showAddDialog = true) }
                } else {
                    _uiState.update { it.copy(editingLink = null, newTitle = "", newUrl = "", newTag = "#COMMON#", showAddDialog = false) }
                }
            }
            is LinksEvent.RequestDelete -> _uiState.update { it.copy(linkToDelete = event.link) }
            LinksEvent.ConfirmDelete -> confirmDelete()
            is LinksEvent.ToggleMenu -> _uiState.update { it.copy(menuExpanded = event.expanded) }
            is LinksEvent.AddLink, LinksEvent.SaveLink -> saveLink()
            LinksEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        val gid = settingsManager.getCurrentGroupId() ?: return
        performRefresh(
            onRefreshStart = {
                manager.save(emptyList(), gid)
                _uiState.update { it.copy(links = emptyList()) }
            }
        ) {
            val snapshot = getLinksColl(gid).get(Source.SERVER).await()
            val list = snapshot.documents.mapNotNull { it.toObject(LinkItem::class.java)?.copy(id = it.id) }
            _uiState.update { it.copy(links = list) }
            manager.save(list, gid)
        }
    }

    private fun confirmDelete() {
        val link = _uiState.value.linkToDelete ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            getLinksColl(gid).document(link.id).delete().await()
            _uiState.update { it.copy(linkToDelete = null) }
        }
    }

    private fun saveLink() {
        val state = _uiState.value
        val title = state.newTitle.trim()
        val rawUrl = state.newUrl.trim()
        val gid = settingsManager.getCurrentGroupId() ?: return
        if (title.isNotBlank() && rawUrl.isNotBlank()) {
            var url = rawUrl
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            val editing = state.editingLink
            val id = editing?.id ?: System.currentTimeMillis().toString()
            val linkItem = LinkItem(
                id = id,
                title = title,
                url = url,
                tag = state.newTag.ifBlank { "#COMMON#" },
                lastUpdatedBy = settingsManager.getAppUserId(),
                timestamp = editing?.timestamp ?: System.currentTimeMillis()
            )
            viewModelScope.launch {
                getLinksColl(gid).document(id).set(linkItem).await()
                _uiState.update { it.copy(newTitle = "", newUrl = "", editingLink = null, showAddDialog = false) }
            }
        }
    }
}
