package com.jtool.android.features.settings.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModel(
    private val groupSyncManager: GroupSyncManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
        observeGroups()
    }

    private fun loadGroups() {
        _uiState.update { it.copy(
            groups = settingsManager.getGroups(),
            currentGroupId = settingsManager.getCurrentGroupId()
        ) }
    }

    private fun observeGroups() {
        groupSyncManager.userGroupsFlow()
            .flatMapLatest { ids ->
                groupSyncManager.groupInfoFlow(ids)
            }
            .onEach { liveGroups ->
                settingsManager.saveGroups(liveGroups)
                _uiState.update { it.copy(groups = liveGroups) }
            }
            .launchIn(viewModelScope)
            
        groupSyncManager.currentGroupIdFlow()
            .onEach { gid ->
                // Only update if we get a real value, or if we were intentionally null.
                // This prevents initial 'null' from overwriting local state before fetch/push finishes.
                if (gid != null || _uiState.value.currentGroupId == null) {
                    _uiState.update { it.copy(currentGroupId = gid) }
                    if (gid != null) settingsManager.setCurrentGroupId(gid)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GroupsEvent) {
        when (event) {
            is GroupsEvent.SwitchGroup -> switchGroup(event.groupId)
            is GroupsEvent.CreateGroup -> createGroup(event.name)
            is GroupsEvent.JoinGroup -> joinGroup(event.id)
            is GroupsEvent.LeaveGroup -> leaveGroup(event.id)
            is GroupsEvent.DeleteGroup -> deleteGroup(event.id)
            is GroupsEvent.UpdateGroupName -> updateGroupName(event.id, event.newName)
            is GroupsEvent.SendInvite -> sendInvite(event.email, event.groupId)
            GroupsEvent.ManualSync -> manualSync()
            GroupsEvent.DismissCreatedDialog -> _uiState.update { it.copy(lastCreatedGroupId = null) }
            GroupsEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Re-fetch everything from server
            groupSyncManager.fetchUserSettings()
            loadGroups()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun switchGroup(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupSyncManager.switchGroup(id)
            _uiState.update { it.copy(currentGroupId = id, isLoading = false) }
        }
    }

    private fun createGroup(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = groupSyncManager.createGroup(name)
            if (result != null) {
                // Automatically join the newly created group
                if (groupSyncManager.joinGroup(result)) {
                    _uiState.update { it.copy(
                        currentGroupId = result,
                        lastCreatedGroupId = result
                    ) }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun joinGroup(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (groupSyncManager.joinGroup(id)) {
                _uiState.update { it.copy(currentGroupId = id, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun leaveGroup(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupSyncManager.leaveGroup(id)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun deleteGroup(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupSyncManager.deleteGroupPermanently(id)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateGroupName(id: String, newName: String) {
        viewModelScope.launch {
            groupSyncManager.updateGroupName(id, newName)
        }
    }

    private fun sendInvite(email: String, groupId: String) {
        viewModelScope.launch {
            groupSyncManager.sendInvite(email, groupId)
        }
    }

    private fun manualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            groupSyncManager.manualSyncMerge()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
