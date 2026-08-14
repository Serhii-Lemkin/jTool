package com.jtool.android.features.notes

import com.jtool.android.core.models.NoteItem

sealed interface NotesEvent {
    data class UpdateNewNoteText(val text: String) : NotesEvent
    data class RequestDelete(val note: NoteItem?) : NotesEvent
    object ConfirmDelete : NotesEvent
    object AddNote : NotesEvent
}
