package com.jtool.android.features.settings.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RequestsViewModel(
    private val groupSyncManager: GroupSyncManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestsUiState())
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    fun onEvent(event: RequestsEvent) {
        when (event) {
            is RequestsEvent.UpdateText -> {
                _uiState.update { it.copy(requestText = event.text) }
            }
            RequestsEvent.SendRequest -> sendRequest()
            RequestsEvent.ResetStatus -> {
                _uiState.update { it.copy(isSuccess = false) }
            }
        }
    }

    private fun sendRequest() {
        val text = _uiState.value.requestText
        if (text.isBlank()) return

        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            groupSyncManager.pushSuggestion(text, settingsManager.getAppUserId())
            _uiState.update { it.copy(isSending = false, isSuccess = true, requestText = "") }
        }
    }
}
