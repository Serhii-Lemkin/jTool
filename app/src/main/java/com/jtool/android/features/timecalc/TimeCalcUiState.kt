package com.jtool.android.features.timecalc

import java.time.LocalDateTime

data class TimeCalcUiState(
    val currentDateTime: LocalDateTime = LocalDateTime.now(),
    val history: List<HistoryItem> = emptyList(),
    val bY: String = "",
    val bMo: String = "",
    val bD: String = "",
    val bH: String = "",
    val bMi: String = "",
    val yearsInput: String = "",
    val monthsInput: String = "",
    val daysInput: String = "",
    val hoursInput: String = "",
    val minutesInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
