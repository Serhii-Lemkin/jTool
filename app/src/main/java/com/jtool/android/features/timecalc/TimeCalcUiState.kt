package com.jtool.android.features.timecalc

import com.jtool.android.core.localization.Loc
import java.time.LocalDateTime

data class HistoryItem(
    val baseDate: LocalDateTime,
    val operation: String,
    val y: String, val mo: String, val d: String, val h: String, val mi: String
) {
    val summary: String get() {
        val parts = mutableListOf<String>()
        if (y.isNotEmpty()) parts.add("${y}${Loc.General.yearShort}")
        if (mo.isNotEmpty()) parts.add("${mo}${Loc.General.monthShort}")
        if (d.isNotEmpty()) parts.add("${d}${Loc.General.dayShort}")
        if (h.isNotEmpty()) parts.add("${h}${Loc.General.hourSymbol}")
        if (mi.isNotEmpty()) parts.add("${mi}${Loc.General.minuteSymbol}")
        return if (parts.isEmpty()) Loc.General.zeroUnits else parts.joinToString(" ")
    }
}

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
