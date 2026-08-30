package com.jtool.android.features.workday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class WorkdayViewModel(
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkdayUiState())
    val uiState: StateFlow<WorkdayUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val today = LocalDate.now()
        val weeklyHours = settingsManager.getAllWeeklyHours()
        val currentDay = today.dayOfWeek
        val requiredHours = weeklyHours[currentDay] ?: 0.0
        
        _uiState.update { it.copy(
            weeklyHours = weeklyHours,
            currentDay = currentDay,
            requiredHours = requiredHours
        ) }

        settingsManager.getRememberedStart(today.toString())?.let { remembered ->
            _uiState.update { it.copy(
                inputTime = remembered,
                isTimeSaved = true
            ) }
            calculateFinishTime(remembered)
        }
        
        if (weeklyHours.values.all { it == 0.0 }) {
            _uiState.update { it.copy(showEditDialog = true) }
        }
    }

    fun onEvent(event: WorkdayEvent) {
        when (event) {
            is WorkdayEvent.UpdateInputTime -> {
                _uiState.update { it.copy(inputTime = event.time, errorText = "") }
            }
            WorkdayEvent.Calculate -> {
                calculateFinishTime(_uiState.value.inputTime)
            }
            WorkdayEvent.ToggleRemember -> {
                if (_uiState.value.isTimeSaved) {
                    _uiState.update { it.copy(showForgetConfirm = true) }
                } else {
                    settingsManager.saveRememberedStart(_uiState.value.inputTime, LocalDate.now().toString())
                    _uiState.update { it.copy(isTimeSaved = true) }
                    calculateFinishTime(_uiState.value.inputTime)
                }
            }
            is WorkdayEvent.ToggleForgetConfirm -> {
                _uiState.update { it.copy(showForgetConfirm = event.show) }
            }
            WorkdayEvent.ConfirmForget -> {
                settingsManager.clearRememberedStart()
                _uiState.update { it.copy(isTimeSaved = false, showForgetConfirm = false) }
            }
            is WorkdayEvent.ToggleEditDialog -> {
                _uiState.update { it.copy(showEditDialog = event.show) }
            }
            is WorkdayEvent.SaveWeeklyHours -> {
                event.hours.forEach { (day, h) -> settingsManager.saveDayHours(day, h) }
                val currentDay = LocalDate.now().dayOfWeek
                _uiState.update { it.copy(
                    weeklyHours = event.hours,
                    showEditDialog = false,
                    requiredHours = event.hours[currentDay] ?: 0.0
                ) }
                viewModelScope.launch {
                    dataManager.pushUserSettings()
                }
            }
        }
    }

    private fun calculateFinishTime(input: String) {
        val parsed = parseTime(input)
        if (parsed != null) {
            val totalMinutes = (_uiState.value.requiredHours * 60).toLong()
            _uiState.update { it.copy(finishTime = parsed.plusMinutes(totalMinutes), errorText = "") }
        } else {
            _uiState.update { it.copy(finishTime = null, errorText = Loc.worktime_invalidFormat) }
        }
    }

    private fun parseTime(input: String): LocalTime? {
        val cleanInput = input.trim().replace(":", "").replace(".", "").replace(",", "")
        
        return when (cleanInput.length) {
            1 -> try { LocalTime.of(cleanInput.toInt(), 0) } catch (_: Exception) { null }
            2 -> try {
                val h = cleanInput.toInt()
                if (h < 24) LocalTime.of(h, 0) else null
            } catch (_: Exception) { null }
            3 -> try {
                val h = cleanInput.substring(0, 1).toInt()
                val m = cleanInput.substring(1).toInt()
                if (m < 60) LocalTime.of(h, m) else null
            } catch (_: Exception) { null }
            4 -> try {
                val h = cleanInput.substring(0, 2).toInt()
                val m = cleanInput.substring(2).toInt()
                if (h < 24 && m < 60) LocalTime.of(h, m) else null
            } catch (_: Exception) { null }
            else -> null
        }
    }
}
