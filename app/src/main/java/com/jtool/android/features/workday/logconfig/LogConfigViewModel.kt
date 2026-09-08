package com.jtool.android.features.workday.logconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.workday.DayLogHours
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class LogConfigViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogConfigUiState())
    val uiState: StateFlow<LogConfigUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val options = settingsManager.getWorktimeOptions()
        val schedule = options.logSchedule.mapKeys { DayOfWeek.valueOf(it.key) }
        val startDay = try { DayOfWeek.valueOf(options.weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }

        _uiState.update { it.copy(
            weeklySchedule = schedule,
            tempWeeklySchedule = schedule,
            weeklyStartDay = startDay,
            useStableApproximateHours = options.useStableApproximateHours,
            stableApproximateStart = options.stableApproximateStart,
            stableApproximateEnd = options.stableApproximateEnd,
            pendingUseStable = options.useStableApproximateHours,
            pendingStableStart = options.stableApproximateStart,
            pendingStableEnd = options.stableApproximateEnd,
            pendingWeeklySchedule = schedule
        ) }
    }

    fun onEvent(event: LogConfigEvent) {
        when (event) {
            is LogConfigEvent.UpdateTempSchedule -> {
                val newTemp = _uiState.value.tempWeeklySchedule + (event.day to event.logHours)
                _uiState.update { it.copy(tempWeeklySchedule = newTemp, hasUnsavedChanges = true) }
            }
            LogConfigEvent.Save -> {
                val current = _uiState.value
                val options = settingsManager.getWorktimeOptions().copy(
                    logSchedule = current.tempWeeklySchedule.mapKeys { it.key.name },
                    isLogHoursSet = current.tempWeeklySchedule.isNotEmpty(),
                    useStableApproximateHours = current.useStableApproximateHours,
                    stableApproximateStart = current.stableApproximateStart,
                    stableApproximateEnd = current.stableApproximateEnd
                )
                settingsManager.saveWorktimeOptions(options)
                _uiState.update { it.copy(weeklySchedule = current.tempWeeklySchedule, hasUnsavedChanges = false) }
                
                // Use non-cancellable scope for pushing settings to ensure it completes after navigation
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    dataManager.pushUserSettings()
                }
            }
            is LogConfigEvent.ToggleExitConfirm -> {
                _uiState.update { it.copy(showExitConfirm = event.show) }
            }
            is LogConfigEvent.ToggleApproximateHoursPicker -> {
                if (event.show) {
                    // Sync pending with current when opening
                    val current = _uiState.value
                    _uiState.update { it.copy(
                        showApproximateHoursPicker = true,
                        pendingUseStable = current.useStableApproximateHours,
                        pendingStableStart = current.stableApproximateStart,
                        pendingStableEnd = current.stableApproximateEnd,
                        pendingWeeklySchedule = current.tempWeeklySchedule
                    ) }
                } else {
                    // Commit pending to current when closing
                    val current = _uiState.value
                    _uiState.update { it.copy(
                        showApproximateHoursPicker = false,
                        useStableApproximateHours = current.pendingUseStable,
                        stableApproximateStart = current.pendingStableStart,
                        stableApproximateEnd = current.pendingStableEnd,
                        tempWeeklySchedule = current.pendingWeeklySchedule,
                        hasUnsavedChanges = true
                    ) }
                }
            }
            is LogConfigEvent.UpdateApproximateHoursConfig -> {
                _uiState.update { it.copy(
                    pendingUseStable = event.useStable,
                    pendingStableStart = event.stableStart ?: it.pendingStableStart,
                    pendingStableEnd = event.stableEnd ?: it.pendingStableEnd
                ) }
            }
        }
    }

    fun updatePendingDay(day: DayOfWeek, hours: DayLogHours) {
        val newPending = _uiState.value.pendingWeeklySchedule + (day to hours)
        _uiState.update { it.copy(pendingWeeklySchedule = newPending) }
    }
}
