package com.jtool.android.core.models

import androidx.annotation.Keep

@Keep
enum class NoteType {
    TEXT, CHECKLIST
}

@Keep
data class NoteItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val type: NoteType = NoteType.TEXT,
    val checklist: List<ChecklistItem> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = ""
)
