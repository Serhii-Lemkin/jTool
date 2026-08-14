package com.jtool.android.features.notes

import com.jtool.android.core.models.NoteItem
import com.jtool.android.core.models.NoteType

data class NotesUiState(
    val notes: List<NoteItem> = emptyList(),
    val noteToDelete: NoteItem? = null,
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Navigation & Editing
    val currentScreen: NotesScreenType = NotesScreenType.LIST,
    val editingNote: NoteItem? = null,
    val creatingNoteType: NoteType? = null,
    val isEditMode: Boolean = false,
    val showTypeMenu: Boolean = false,
    
    // Draft state for creating/editing
    val draftTitle: String = "",
    val draftContent: String = "",
    val checklistInput: String = ""
)

enum class NotesScreenType {
    LIST, EDIT
}
