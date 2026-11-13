package com.jtool.android.core.localization.domains

import com.jtool.android.core.localization.Loc

object NotesLoc {
    val notes get() = Loc.s("notes_notes", "Notes")
    val textNote get() = Loc.s("notes_textNote", "Text Note")
    val checklistNote get() = Loc.s("notes_checklistNote", "Checklist")
    val noteContentHint get() = Loc.s("notes_noteContentHint", "Note content...")
    val noteSaved get() = Loc.s("notes_noteSaved", "Note saved!")
    val deleteNoteTitle get() = Loc.s("notes_deleteNoteTitle", "Delete Note?")
    val noteTitlePlaceholder get() = Loc.s("notes_noteTitlePlaceholder", "Title")
    val untitledNote get() = Loc.s("notes_untitledNote", "Untitled")
    val addTitleQuestion get() = Loc.s("notes_addTitleQuestion", "Add a Title?")
    val addTitleExpl get() = Loc.s("notes_addTitleExpl", "Adding a title makes it easier to find this note later. If you don't want one, just press Save.")
    val previewLength get() = Loc.s("notes_previewLength", "Preview Length")
    val previewLengthDesc get() = Loc.s("notes_previewLengthDesc", "Choose how many lines of note text to show in the list")
    val previewLines get() = Loc.s("notes_previewLines", "%d lines")
    val previewOneLine get() = Loc.s("notes_previewOneLine", "1 line")
    val previewNoLimit get() = Loc.s("notes_previewNoLimit", "Full text")
    val editNoteDesc get() = Loc.s("notes_editNoteDesc", "Open the note in edit mode")
    val deleteNoteDesc get() = Loc.s("notes_deleteNoteDesc", "Permanently delete this note")
}
