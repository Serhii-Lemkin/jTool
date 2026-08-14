package com.jtool.android.features.settings.visibility

import androidx.lifecycle.ViewModel
import com.jtool.android.ui.navigation.AppProperties
import com.jtool.android.core.settings.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppVisibilityViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppVisibilityUiState())
    val uiState: StateFlow<AppVisibilityUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val visibilityMap = AppProperties.allApps.associate { it.id to settingsManager.isAppVisible(it.id) }
        _uiState.update { it.copy(
            isGridLayout = settingsManager.isGridLayout(),
            visibleApps = visibilityMap
        ) }
    }

    fun onEvent(event: AppVisibilityEvent) {
        when (event) {
            is AppVisibilityEvent.ToggleAppVisibility -> {
                settingsManager.setAppVisible(event.appId, event.visible)
                _uiState.update { it.copy(
                    visibleApps = it.visibleApps + (event.appId to event.visible)
                ) }
            }
            AppVisibilityEvent.ToggleGridLayout -> {
                val newGrid = !settingsManager.isGridLayout()
                settingsManager.setGridLayout(newGrid)
                _uiState.update { it.copy(isGridLayout = newGrid) }
            }
        }
    }
}
