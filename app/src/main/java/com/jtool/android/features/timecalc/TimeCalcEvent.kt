package com.jtool.android.features.timecalc

sealed interface TimeCalcEvent {
    object ResetToNow : TimeCalcEvent
    data class UpdateBase(
        val y: String? = null,
        val mo: String? = null,
        val d: String? = null,
        val h: String? = null,
        val mi: String? = null
    ) : TimeCalcEvent
    data class UpdateOperation(
        val y: String? = null,
        val mo: String? = null,
        val d: String? = null,
        val h: String? = null,
        val mi: String? = null
    ) : TimeCalcEvent
    data class PerformOperation(val isAddition: Boolean) : TimeCalcEvent
    data class RestoreFromHistory(val item: HistoryItem) : TimeCalcEvent
}
