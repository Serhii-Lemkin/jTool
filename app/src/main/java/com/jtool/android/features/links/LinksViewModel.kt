package com.jtool.android.features.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.LinkItem
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LinksViewModel(
    private val manager: JLinksManager,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinksUiState())
    val uiState: StateFlow<LinksUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeSyncData()
    }

    private fun loadInitialData() {
        val links = manager.loadLinks()
        _uiState.update { it.copy(links = links) }
    }

    private fun observeSyncData() {
        val currentGroupId = settingsManager.getCurrentGroupId()
        if (currentGroupId != null) {
            viewModelScope.launch {
                groupSyncManager.fetchUserSettings()
            }
            
            groupSyncManager.groupDataFlow(currentGroupId)
                .onEach { 
                    val links = manager.loadLinks()
                    _uiState.update { it.copy(links = links) }
                }
                .launchIn(viewModelScope)

            groupSyncManager.userNamesFlow()
                .onEach { names ->
                    _uiState.update { it.copy(userNames = names) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onEvent(event: LinksEvent) {
        when (event) {
            is LinksEvent.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is LinksEvent.UpdateNewTitle -> {
                _uiState.update { it.copy(newTitle = event.title) }
            }
            is LinksEvent.UpdateNewUrl -> {
                _uiState.update { it.copy(newUrl = event.url) }
            }
            is LinksEvent.ToggleAddDialog -> {
                _uiState.update { it.copy(showAddDialog = event.show) }
            }
            is LinksEvent.RequestDelete -> {
                _uiState.update { it.copy(linkToDelete = event.link) }
            }
            LinksEvent.ConfirmDelete -> {
                val link = _uiState.value.linkToDelete
                if (link != null) {
                    val newList = _uiState.value.links.filter { it.id != link.id }
                    updateLinks(newList)
                    _uiState.update { it.copy(linkToDelete = null) }
                }
            }
            is LinksEvent.ToggleMenu -> {
                _uiState.update { it.copy(menuExpanded = event.expanded) }
            }
            LinksEvent.AddLink -> {
                val title = _uiState.value.newTitle
                val rawUrl = _uiState.value.newUrl
                if (title.isNotBlank() && rawUrl.isNotBlank()) {
                    var url = rawUrl.trim()
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    val newLink = LinkItem(
                        id = System.currentTimeMillis(),
                        title = title.trim(),
                        url = url,
                        lastUpdatedBy = settingsManager.getAppUserId()
                    )
                    updateLinks(_uiState.value.links + newLink)
                    _uiState.update { it.copy(newTitle = "", newUrl = "", showAddDialog = false) }
                }
            }
        }
    }

    private fun updateLinks(newList: List<LinkItem>) {
        _uiState.update { it.copy(links = newList) }
        manager.saveLinks(newList)
        triggerSync()
    }

    private fun triggerSync() {
        settingsManager.setSyncPending(true)
        viewModelScope.launch {
            groupSyncManager.pushGroupData()
        }
    }
}
