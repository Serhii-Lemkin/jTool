package com.jtool.android.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminViewModel(
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        groupSyncManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)

        groupSyncManager.allSuggestionsFlow()
            .onEach { requests -> _uiState.update { it.copy(requests = requests) } }
            .launchIn(viewModelScope)

        groupSyncManager.allUsersFlow()
            .onEach { users -> _uiState.update { it.copy(users = users) } }
            .launchIn(viewModelScope)

        groupSyncManager.allGroupsFlow()
            .onEach { groups -> _uiState.update { it.copy(groups = groups) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AdminEvent) {
        when (event) {
            is AdminEvent.Navigate -> {
                _uiState.update { it.copy(currentSubScreen = event.subScreen) }
            }
            is AdminEvent.SelectRequest -> {
                _uiState.update { it.copy(selectedRequest = event.request, currentSubScreen = if (event.request != null) "request_detail" else "requests") }
            }
            is AdminEvent.DeleteRequest -> {
                viewModelScope.launch {
                    groupSyncManager.deleteSuggestion(event.id)
                    _uiState.update { it.copy(selectedRequest = null, currentSubScreen = "requests") }
                }
            }
            is AdminEvent.UpdateUserSearchQuery -> {
                _uiState.update { it.copy(userSearchQuery = event.query) }
            }
            AdminEvent.ApplyUserSearch -> {
                _uiState.update { it.copy(appliedUserSearchQuery = it.userSearchQuery) }
            }
            is AdminEvent.SetUserAdminStatus -> {
                viewModelScope.launch {
                    groupSyncManager.setUserAdminStatus(event.uid, event.isAdmin)
                }
            }
            is AdminEvent.RequestDeleteUser -> {
                if (event.step == 1) {
                    _uiState.update { it.copy(showUserDeleteConfirm1 = event.uid) }
                } else {
                    _uiState.update { it.copy(showUserDeleteConfirm1 = null, showUserDeleteConfirm2 = event.uid) }
                }
            }
            is AdminEvent.ConfirmDeleteUser -> {
                viewModelScope.launch {
                    groupSyncManager.deleteUserPermanently(event.uid)
                    _uiState.update { it.copy(showUserDeleteConfirm2 = null) }
                }
            }
            is AdminEvent.UpdateGroupSearchQuery -> {
                _uiState.update { it.copy(groupSearchQuery = event.query) }
            }
            AdminEvent.ApplyGroupSearch -> {
                _uiState.update { it.copy(appliedGroupSearchQuery = it.groupSearchQuery) }
            }
            is AdminEvent.RequestDeleteGroup -> {
                if (event.step == 1) {
                    _uiState.update { it.copy(showGroupDeleteConfirm1 = event.gid) }
                } else {
                    _uiState.update { it.copy(showGroupDeleteConfirm1 = null, showGroupDeleteConfirm2 = event.gid) }
                }
            }
            is AdminEvent.ConfirmDeleteGroup -> {
                viewModelScope.launch {
                    groupSyncManager.deleteGroupPermanently(event.gid)
                    _uiState.update { it.copy(showGroupDeleteConfirm2 = null) }
                }
            }
            AdminEvent.StartRTDBMigration -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isMigrating = true, migrationStatus = "Starting migration...") }
                    val success = groupSyncManager.migrateFromRTDB { status ->
                        _uiState.update { it.copy(migrationStatus = status) }
                    }
                    _uiState.update { it.copy(
                        isMigrating = false, 
                        migrationStatus = if (success) "Migration completed successfully!" else it.migrationStatus
                    ) }
                }
            }
        }
    }
}
