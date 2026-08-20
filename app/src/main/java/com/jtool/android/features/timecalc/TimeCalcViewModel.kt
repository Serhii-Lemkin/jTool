package com.jtool.android.features.timecalc

import androidx.lifecycle.ViewModel
import com.jtool.android.core.localization.Loc
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime

class TimeCalcViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TimeCalcUiState())
    val uiState: StateFlow<TimeCalcUiState> = _uiState.asStateFlow()

    init {
        onEvent(TimeCalcEvent.ResetToNow)
    }

    fun onEvent(event: TimeCalcEvent) {
        when (event) {
            TimeCalcEvent.ResetToNow -> {
                val now = LocalDateTime.now()
                updateStateWithDateTime(now)
            }
            is TimeCalcEvent.UpdateBase -> {
                val newY = event.y ?: _uiState.value.bY
                val newMo = event.mo ?: _uiState.value.bMo
                val newD = event.d ?: _uiState.value.bD
                val newH = event.h ?: _uiState.value.bH
                val newMi = event.mi ?: _uiState.value.bMi
                
                try {
                    val year = newY.toIntOrNull() ?: 2024
                    val month = (newMo.toIntOrNull() ?: 1).coerceIn(1, 12)
                    val hour = (newH.toIntOrNull() ?: 0).coerceIn(0, 23)
                    val min = (newMi.toIntOrNull() ?: 0).coerceIn(0, 59)
                    
                    val tempDate = LocalDateTime.of(year, month, 1, hour, min)
                    val day = (newD.toIntOrNull() ?: 1).coerceIn(1, tempDate.toLocalDate().lengthOfMonth())
                    
                    val next = LocalDateTime.of(year, month, day, hour, min)
                    _uiState.update { it.copy(
                        currentDateTime = next,
                        bY = newY, bMo = newMo, bD = newD, bH = newH, bMi = newMi
                    ) }
                } catch (_: Exception) {
                    _uiState.update { it.copy(bY = newY, bMo = newMo, bD = newD, bH = newH, bMi = newMi) }
                }
            }
            is TimeCalcEvent.UpdateOperation -> {
                _uiState.update { it.copy(
                    yearsInput = event.y ?: it.yearsInput,
                    monthsInput = event.mo ?: it.monthsInput,
                    daysInput = event.d ?: it.daysInput,
                    hoursInput = event.h ?: it.hoursInput,
                    minutesInput = event.mi ?: it.minutesInput
                ) }
            }
            is TimeCalcEvent.PerformOperation -> {
                val state = _uiState.value
                val next = calculateNewDate(
                    state.currentDateTime,
                    state.yearsInput,
                    state.monthsInput,
                    state.daysInput,
                    state.hoursInput,
                    state.minutesInput,
                    event.isAddition
                )
                val historyItem = HistoryItem(
                    state.currentDateTime,
                    if (event.isAddition) Loc.timecalc_added else Loc.timecalc_subtracted,
                    state.yearsInput, state.monthsInput, state.daysInput, state.hoursInput, state.minutesInput
                )
                _uiState.update { it.copy(
                    currentDateTime = next,
                    history = it.history + historyItem,
                    yearsInput = "", monthsInput = "", daysInput = "", hoursInput = "", minutesInput = "",
                    bY = next.year.toString(),
                    bMo = next.monthValue.toString(),
                    bD = next.dayOfMonth.toString(),
                    bH = next.hour.toString(),
                    bMi = next.minute.toString()
                ) }
            }
            is TimeCalcEvent.RestoreFromHistory -> {
                updateStateWithDateTime(event.item.baseDate)
            }
        }
    }

    private fun updateStateWithDateTime(dateTime: LocalDateTime) {
        _uiState.update { it.copy(
            currentDateTime = dateTime,
            bY = dateTime.year.toString(),
            bMo = dateTime.monthValue.toString(),
            bD = dateTime.dayOfMonth.toString(),
            bH = dateTime.hour.toString(),
            bMi = dateTime.minute.toString()
        ) }
    }

    private fun calculateNewDate(base: LocalDateTime, y: String, mo: String, d: String, h: String, mi: String, isAddition: Boolean): LocalDateTime {
        var result = base
        val m = if (isAddition) 1L else -1L
        if (y.isNotEmpty()) result = result.plusYears(y.toLong() * m)
        if (mo.isNotEmpty()) result = result.plusMonths(mo.toLong() * m)
        if (d.isNotEmpty()) result = result.plusDays(d.toLong() * m)
        if (h.isNotEmpty()) result = result.plusHours(h.toLong() * m)
        if (mi.isNotEmpty()) result = result.plusMinutes(mi.toLong() * m)
        return result
    }
}
