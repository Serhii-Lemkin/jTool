package com.jtool.android.features.settings.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModel(
    private val dataManager: DataManager,
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
        dataManager.userGroupsFlow()
            .flatMapLatest { ids ->
                dataManager.groupInfoFlow(ids)
            }
            .onEach { liveGroups ->
                settingsManager.saveGroups(liveGroups)
                _uiState.update { it.copy(groups = liveGroups) }
            }
            .launchIn(viewModelScope)
            
        dataManager.currentGroupIdFlow()
            .onEach { gid ->
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
            dataManager.fetchUserSettings()
            loadGroups()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun switchGroup(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dataManager.switchGroup(id)
            _uiState.update { it.copy(currentGroupId = id, isLoading = false) }
        }
    }

    private fun createGroup(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = dataManager.createGroup(name)
            if (result != null) {
                if (dataManager.joinGroup(result)) {
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
            if (dataManager.joinGroup(id)) {
                _uiState.update { it.copy(currentGroupId = id, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun leaveGroup(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dataManager.leaveGroup(id)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun deleteGroup(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dataManager.deleteGroupPermanently(id)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateGroupName(id: String, newName: String) {
        viewModelScope.launch {
            dataManager.updateGroupName(id, newName)
        }
    }

    private fun sendInvite(email: String, groupId: String) {
        viewModelScope.launch {
            dataManager.sendInvite(email, groupId)
        }
    }

    private fun manualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dataManager.manualSyncMerge()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
