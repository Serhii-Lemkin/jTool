package com.jtool.android.features.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authManager: AuthManager,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val user = authManager.getCurrentUser()
        _uiState.update { it.copy(
            currentUserEmail = user?.email,
            userName = settingsManager.getUserName()
        ) }
    }

    fun onEvent(event: AccountEvent) {
        when (event) {
            is AccountEvent.UpdateUserName -> updateUserName(event.name)
            AccountEvent.SignOut -> signOut()
            AccountEvent.DeleteAccount -> deleteAccount()
        }
    }

    private fun updateUserName(name: String) {
        settingsManager.saveUserName(name)
        _uiState.update { it.copy(userName = name) }
        viewModelScope.launch { groupSyncManager.pushUserSettings() }
    }

    private fun signOut() {
        authManager.signOut()
        settingsManager.setCurrentGroupId(null)
        settingsManager.clearAllGroups()
        _uiState.update { it.copy(currentUserEmail = null) }
    }

    private fun deleteAccount() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val success = groupSyncManager.deleteMyAccount()
            _uiState.update { it.copy(isLoading = false) }
            if (success) {
                // Nav will be handled by UI observing currentUserEmail becoming null
                _uiState.update { it.copy(currentUserEmail = null) }
            }
        }
    }
}
