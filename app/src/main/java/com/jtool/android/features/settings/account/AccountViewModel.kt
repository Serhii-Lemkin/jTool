package com.jtool.android.features.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authManager: AuthManager,
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeAuthState()
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        val user = authManager.getCurrentUser()
        if (user != null && settingsManager.getUserName().isEmpty() && !user.displayName.isNullOrEmpty()) {
            settingsManager.saveUserName(user.displayName!!)
        }
        _uiState.update { it.copy(
            currentUserEmail = user?.email,
            userName = settingsManager.getUserName()
        ) }
    }

    private fun observeAuthState() {
        authManager.authStateFlow()
            .onEach { user ->
                if (user != null) {
                    if (settingsManager.getUserName().isEmpty() && !user.displayName.isNullOrEmpty()) {
                        settingsManager.saveUserName(user.displayName!!)
                    }
                }
                _uiState.update { it.copy(
                    currentUserEmail = user?.email,
                    userName = if (user != null) settingsManager.getUserName() else ""
                ) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AccountEvent) {
        when (event) {
            is AccountEvent.UpdateUserName -> updateUserName(event.name)
            AccountEvent.SignOut -> signOut()
            AccountEvent.DeleteAccount -> deleteAccount()
        }
    }

    private fun updateUserName(name: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            settingsManager.saveUserName(name)
            _uiState.update { it.copy(userName = name) }
            dataManager.pushUserSettings()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun signOut() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authManager.signOut()
            settingsManager.setCurrentGroupId(null)
            settingsManager.clearAllGroups()
            _uiState.update { it.copy(currentUserEmail = null, isLoading = false) }
        }
    }

    private fun deleteAccount() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val success = dataManager.deleteMyAccount()
            _uiState.update { it.copy(isLoading = false) }
            if (success) {
                _uiState.update { it.copy(currentUserEmail = null) }
            }
        }
    }
}
