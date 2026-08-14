package com.jtool.android.features.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.jtool.android.core.models.LinkItem
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LinksViewModel(
    private val manager: LinksManager,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(LinksUiState())
    val uiState: StateFlow<LinksUiState> = _uiState.asStateFlow()

    private var linksListener: ListenerRegistration? = null

    init {
        observeGroupId()
        observeSyncData()
    }

    private fun observeGroupId() {
        groupSyncManager.currentGroupIdFlow()
            .onEach { gid ->
                _uiState.update { it.copy(links = emptyList()) }
                linksListener?.remove()
                val localLinks = manager.loadLinks()
                _uiState.update { it.copy(links = localLinks) }
                if (gid != null) {
                    observeFirestoreLinks(gid)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getLinksColl(gid: String) = firestore.collection("groups").document(gid).collection("links")

    private fun observeFirestoreLinks(gid: String) {
        linksListener = getLinksColl(gid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(LinkItem::class.java)?.copy(id = doc.id)
                }
                _uiState.update { it.copy(links = list) }
                manager.saveLinks(list)
            }
        }
    }

    private fun observeSyncData() {
        groupSyncManager.userNamesFlow()
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

    override fun onCleared() {
        super.onCleared()
        linksListener?.remove()
    }
}
