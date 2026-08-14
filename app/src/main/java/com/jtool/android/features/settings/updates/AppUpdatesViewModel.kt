package com.jtool.android.features.settings.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppUpdatesViewModel(
    private val backupManager: BackupManager,
    private val groupSyncManager: GroupSyncManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdatesUiState())
    val uiState: StateFlow<AppUpdatesUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(
            currentVersionCode = backupManager.getCurrentVersionCode(),
            currentVersionName = backupManager.getCurrentVersionName()
        ) }
        checkUpdates()
    }

    fun onEvent(event: AppUpdatesEvent) {
        when (event) {
            AppUpdatesEvent.CheckUpdates -> checkUpdates()
        }
    }

    private fun checkUpdates() {
        _uiState.update { it.copy(isChecking = true) }
        viewModelScope.launch {
            val appInfo = groupSyncManager.getLatestAppInfo()
            
            val code = when (val v = appInfo?.get("versionCode")) {
                is Long -> v.toInt()
                is Int -> v
                is Number -> v.toInt()
                is String -> v.toIntOrNull()
                else -> null
            }
            
            val name = appInfo?.get("versionName") as? String
            val url = appInfo?.get("downloadUrl") as? String
            
            if (url != null) {
                settingsManager.setUpdateUrl(url)
            }

            _uiState.update { it.copy(
                availableVersionCode = code,
                availableVersionName = name,
                updateUrl = url,
                isChecking = false
            ) }
        }
    }
}
