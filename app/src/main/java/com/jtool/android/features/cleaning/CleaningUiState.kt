package com.jtool.android.features.cleaning

import com.jtool.android.core.models.CleaningTask

data class CleaningUiState(
    val tasks: List<CleaningTask> = emptyList(),
    val showDisabledView: Boolean = false,
    val showAddDialog: Boolean = false,
    val taskToDelete: CleaningTask? = null,
    val editingTask: CleaningTask? = null,
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val displayTasks: List<CleaningTask>
        get() = if (showDisabledView) {
            tasks.filter { !it.isEnabled }
        } else {
            tasks.filter { it.isEnabled }.sortedWith(
                compareBy<CleaningTask> { it.isChecked }
                    .thenByDescending { it.lastDoneTimestamp.coerceAtLeast(it.timestamp) }
            )
        }
}
