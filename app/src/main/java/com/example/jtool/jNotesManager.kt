package com.example.jtool

import android.content.Context

data class NoteItem(
    val id: Long,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "Unknown"
)

class JNotesManager(context: Context) {
    private val prefs = context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE)

    fun saveNotes(notes: List<NoteItem>) {
        val encoded = notes.joinToString(";") { "${it.id}|${it.content.replace("|", "\\|").replace(";", "\\;")}|${it.timestamp}|${it.lastUpdatedBy}" }
        prefs.edit().putString("notes", encoded).apply()
    }

    fun loadNotes(): List<NoteItem> {
        val encoded = prefs.getString("notes", "") ?: ""
        if (encoded.isEmpty()) return emptyList()
        
        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 3) {
                NoteItem(
                    id = parts[0].toLongOrNull() ?: 0L,
                    content = parts[1].replace("\\|", "|").replace("\\;", ";"),
                    timestamp = parts[2].toLongOrNull() ?: 0L,
                    lastUpdatedBy = if (parts.size >= 4) parts[3] else "Unknown"
                )
            } else null
        }
    }
}
