package com.jtool.android.features.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GeneralSettingsViewModel(
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneralSettingsUiState())
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(
            isDarkMode = settingsManager.isDarkMode(),
            language = settingsManager.getLanguage()
        ) }
    }

    fun onEvent(event: GeneralSettingsEvent) {
        when (event) {
            GeneralSettingsEvent.ToggleTheme -> {
                val newMode = !settingsManager.isDarkMode()
                settingsManager.setDarkMode(newMode)
                _uiState.update { it.copy(isDarkMode = newMode) }
                viewModelScope.launch { groupSyncManager.pushUserSettings() }
            }
            is GeneralSettingsEvent.SetLanguage -> {
                settingsManager.setLanguage(event.lang)
                _uiState.update { it.copy(language = event.lang) }
                viewModelScope.launch { groupSyncManager.pushUserSettings() }
            }
        }
    }
}
