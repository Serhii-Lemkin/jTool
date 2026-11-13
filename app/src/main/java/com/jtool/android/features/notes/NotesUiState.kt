package com.jtool.android.features.notes

import com.jtool.android.core.models.feature.notes.NoteItem
import com.jtool.android.core.models.feature.notes.enums.NoteTypeEnum
import com.jtool.android.core.models.feature.notes.enums.NotesScreenTypeEnum

data class NotesUiState(
    val notes: List<NoteItem> = emptyList(),
    val noteToDelete: NoteItem? = null,
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    
    val currentScreen: NotesScreenTypeEnum = NotesScreenTypeEnum.LIST,
    val editingNote: NoteItem? = null,
    val creatingNoteType: NoteTypeEnum? = null,
    val isEditMode: Boolean = false,
    val showTypeMenu: Boolean = false,
    
    val draftTitle: String = "",
    val draftContent: String = "",
    val checklistInput: String = "",
    
    val searchQuery: String = "",
    val filterType: NoteTypeEnum? = null,
    val isSearchVisible: Boolean = false,
    val previewMaxLines: Int = 2
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


