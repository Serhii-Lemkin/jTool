package com.example.jtool

import android.content.Context

data class CleaningTask(
    val id: Long,
    val title: String,
    val frequencyDays: Int,
    val isRecurring: Boolean,
    val lastDoneTimestamp: Long = 0L,
    val isChecked: Boolean = false,
    val isEnabled: Boolean = true,
    val lastUpdatedBy: String = "Unknown",
    val timestamp: Long = System.currentTimeMillis()
)

class JCleaningManager(context: Context) {
    private val prefs = context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE)

    fun saveTasks(tasks: List<CleaningTask>) {
        val encoded = tasks.joinToString(";") { 
            "${it.id}|${it.title.replace("|", "\\|").replace(";", "\\;")}|${it.frequencyDays}|${it.isRecurring}|${it.lastDoneTimestamp}|${it.isChecked}|${it.isEnabled}|${it.lastUpdatedBy}|${it.timestamp}" 
        }
        prefs.edit().putString("tasks", encoded).apply()
    }

    fun loadTasks(): List<CleaningTask> {
        val encoded = prefs.getString("tasks", "") ?: ""
        if (encoded.isEmpty()) return emptyList()
        
        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 8) {
                CleaningTask(
                    id = parts[0].toLongOrNull() ?: 0L,
                    title = parts[1].replace("\\|", "|").replace("\\;", ";"),
                    frequencyDays = parts[2].toIntOrNull() ?: 1,
                    isRecurring = parts[3].toBoolean(),
                    lastDoneTimestamp = parts[4].toLongOrNull() ?: 0L,
                    isChecked = parts[5].toBoolean(),
                    isEnabled = parts[6].toBoolean(),
                    lastUpdatedBy = parts[7],
                    timestamp = if (parts.size >= 9) parts[8].toLongOrNull() ?: 0L else 0L
                )
            } else null
        }
    }
}
