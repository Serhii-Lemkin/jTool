package com.jtool.android.features.settings.updates

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppUpdatesViewModel(
    private val context: Context,
    private val backupManager: BackupManager
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
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(context)
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    _uiState.update { it.copy(
                        availableVersionCode = appUpdateInfo.availableVersionCode(),
                        availableVersionName = "Google Play",
                        isChecking = false
                    ) }
                } else {
                    _uiState.update { it.copy(
                        availableVersionCode = null,
                        availableVersionName = null,
                        isChecking = false
                    ) }
                }
            }.addOnFailureListener {
                _uiState.update { it.copy(isChecking = false) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isChecking = false) }
        }
    }
}
