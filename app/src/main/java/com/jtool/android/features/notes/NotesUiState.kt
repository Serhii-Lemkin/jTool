package com.jtool.android.features.notes

import com.jtool.android.core.models.NoteItem
import com.jtool.android.core.models.NoteType

data class NotesUiState(
    val notes: List<NoteItem> = emptyList(),
    val noteToDelete: NoteItem? = null,
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    
    // Navigation & Editing
    val currentScreen: NotesScreenType = NotesScreenType.LIST,
    val editingNote: NoteItem? = null,
    val creatingNoteType: NoteType? = null,
    val isEditMode: Boolean = false,
    val showTypeMenu: Boolean = false,
    
    // Draft state for creating/editing
    val draftTitle: String = "",
    val draftContent: String = "",
    val checklistInput: String = "",
    
    // Search & Filter
    val searchQuery: String = "",
    val filterType: NoteType? = null, // null means "All"
    val isSearchVisible: Boolean = false
) {
    val filteredNotes: List<NoteItem>
        get() = notes
            .filter { if (filterType == null) true else it.type == filterType }
            .filter {
                if (searchQuery.isBlank()) true
                else {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true) ||
                    it.checklist.any { item -> item.text.contains(searchQuery, ignoreCase = true) }
                }
            }
            .sortedByDescending { it.timestamp }
}

enum class NotesScreenType {
    LIST, EDIT
}
