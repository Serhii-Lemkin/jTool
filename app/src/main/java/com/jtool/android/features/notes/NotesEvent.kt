package com.jtool.android.features.notes

import com.jtool.android.core.models.feature.notes.NoteItem
import com.jtool.android.core.models.feature.notes.enums.NoteTypeEnum

sealed interface NotesEvent {
    data class RequestDelete(val note: NoteItem?) : NotesEvent
    object ConfirmDelete : NotesEvent
    data class ToggleTypeMenu(val show: Boolean) : NotesEvent
    data class StartNewNote(val type: NoteTypeEnum) : NotesEvent
    data class OpenNote(val note: NoteItem, val editMode: Boolean) : NotesEvent
    
    data class UpdateDraftTitle(val title: String) : NotesEvent
    data class UpdateDraftContent(val content: String) : NotesEvent
    data class UpdateChecklistInput(val text: String) : NotesEvent
    object AddChecklistItem : NotesEvent
    data class ToggleChecklistItem(val id: String) : NotesEvent
    data class RemoveChecklistItem(val id: String) : NotesEvent
    object SaveNote : NotesEvent
    object NavigateBack : NotesEvent
    object ToggleEditMode : NotesEvent
    
    data class AddComment(val content: String, val parentId: String?) : NotesEvent
    data class EditComment(val commentId: String, val newContent: String) : NotesEvent
    data class DeleteComment(val commentId: String) : NotesEvent
    object Refresh : NotesEvent
    
    object ToggleSearch : NotesEvent
    data class UpdateSearchQuery(val query: String) : NotesEvent
    data class SetFilterType(val type: NoteTypeEnum?) : NotesEvent
    data class SetPreviewMaxLines(val lines: Int) : NotesEvent
}
