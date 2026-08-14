package com.jtool.android.features.workday

import java.time.DayOfWeek
import java.time.LocalTime

data class WorkdayUiState(
    val weeklyHours: Map<DayOfWeek, Double> = emptyMap(),
    val showForgetConfirm: Boolean = false,
    val showEditDialog: Boolean = false,
    val isTimeSaved: Boolean = false,
    val inputTime: String = "",
    val finishTime: LocalTime? = null,
    val errorText: String = "",
    val currentDay: DayOfWeek = DayOfWeek.MONDAY,
    val requiredHours: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)
