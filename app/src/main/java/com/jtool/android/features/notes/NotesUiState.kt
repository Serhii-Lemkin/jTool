package com.jtool.android.features.notes

import com.jtool.android.core.models.NoteItem

data class NotesUiState(
    val notes: List<NoteItem> = emptyList(),
    val newNoteText: String = "",
    val noteToDelete: NoteItem? = null,
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)
