package com.jtool.android.features.links

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
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
                        android.util.Log.e("LinksVM", "Error deserializing link ${doc.id}", e)
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
            is LinksEvent.ToggleAddDialog -> _uiState.update { it.copy(showAddDialog = event.show) }
            is LinksEvent.RequestDelete -> _uiState.update { it.copy(linkToDelete = event.link) }
            LinksEvent.ConfirmDelete -> confirmDelete()
            is LinksEvent.ToggleMenu -> _uiState.update { it.copy(menuExpanded = event.expanded) }
            LinksEvent.AddLink -> addLink()
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
            val snapshot = getLinksColl(gid).get(com.google.firebase.firestore.Source.SERVER).await()
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

    private fun addLink() {
        val title = _uiState.value.newTitle.trim()
        val rawUrl = _uiState.value.newUrl.trim()
        val gid = settingsManager.getCurrentGroupId() ?: return
        if (title.isNotBlank() && rawUrl.isNotBlank()) {
            var url = rawUrl
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            val id = System.currentTimeMillis().toString()
            val newLink = LinkItem(
                id = id,
                title = title,
                url = url,
                lastUpdatedBy = settingsManager.getAppUserId(),
                timestamp = System.currentTimeMillis()
            )
            viewModelScope.launch {
                getLinksColl(gid).document(id).set(newLink).await()
                _uiState.update { it.copy(newTitle = "", newUrl = "", showAddDialog = false) }
            }
        }
    }
}
