package com.jtool.android.features.settings.main

import androidx.lifecycle.ViewModel
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.settings.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsMainViewModel(
    private val authManager: AuthManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsMainUiState())
    val uiState: StateFlow<SettingsMainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val user = authManager.getCurrentUser()
        val currentGroupId = settingsManager.getCurrentGroupId()
        _uiState.update { it.copy(
            currentUser = user,
            userEmail = user?.email,
            isSyncMissing = user == null || currentGroupId == null
        ) }
    }
}
