package com.jtool.android.features.cleaning

import android.content.Context
import com.jtool.android.core.models.CleaningTask

class CleaningManager(context: Context) {
    private val context = context
    private fun getPrefs(groupId: String?) = context.getSharedPreferences("jtool_cleaning_${groupId ?: "none"}", Context.MODE_PRIVATE)

    fun saveTasks(tasks: List<CleaningTask>, groupId: String?) {
        val encoded = tasks.joinToString(";") { 
            "${it.id}|${it.title.replace("|", "\\|").replace(";", "\\;")}|${it.frequencyDays}|${it.isRecurring}|${it.lastDoneTimestamp}|${it.isChecked}|${it.isEnabled}|${it.lastUpdatedBy}|${it.timestamp}" 
        }
        getPrefs(groupId).edit().putString("tasks", encoded).apply()
    }

    fun loadTasks(groupId: String?): List<CleaningTask> {
        val encoded = getPrefs(groupId).getString("tasks", "") ?: ""
        if (encoded.isEmpty()) return emptyList()
        
        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 8) {
                CleaningTask(
                    id = parts[0],
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
