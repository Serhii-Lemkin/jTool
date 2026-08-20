package com.jtool.android.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.jtool.android.core.models.ChecklistItem
import com.jtool.android.core.models.Comment
import com.jtool.android.core.models.NoteItem
import com.jtool.android.core.models.NoteType
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotesViewModel(
    private val manager: NotesManager,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var notesListener: ListenerRegistration? = null
    private var syncJob: Job? = null

    init {
        observeGroupId()
        observeUserNames()
    }

    private fun observeGroupId() {
        groupSyncManager.currentGroupIdFlow()
            .onEach { gid ->
                _uiState.update { it.copy(notes = emptyList(), editingNote = null) }
                notesListener?.remove()
                val localNotes = manager.loadNotes()
                _uiState.update { it.copy(notes = localNotes) }
                if (gid != null) {
                    observeFirestoreNotes(gid)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getNotesColl(gid: String) = firestore.collection("groups").document(gid).collection("notes")

    private fun observeFirestoreNotes(gid: String) {
        notesListener = getNotesColl(gid).orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    syncJob?.cancel()
                    syncJob = viewModelScope.launch {
                        val notesList = snapshot.documents.mapNotNull { doc ->
                            val note = doc.toObject(NoteItem::class.java)?.copy(id = doc.id)
                            if (note != null) {
                                val checklist = doc.reference.collection("checklist")
                                    .orderBy("timestamp").get().await()
                                    .toObjects(ChecklistItem::class.java)
                                
                                val commentsSnapshot = doc.reference.collection("comments")
                                    .orderBy("timestamp").get().await()
                                
                                val allComments = commentsSnapshot.documents.mapNotNull { cDoc ->
                                    cDoc.toObject(Comment::class.java)?.copy(id = cDoc.id)
                                }
                                
                                note.copy(checklist = checklist, comments = buildCommentTree(allComments))
                            } else null
                        }
                        _uiState.update { state ->
                            val updatedEditingNote = state.editingNote?.let { editing ->
                                notesList.find { it.id == editing.id }
                            }
                            state.copy(
                                notes = notesList,
                                editingNote = updatedEditingNote ?: state.editingNote
                            )
                        }
                        manager.saveNotes(notesList)
                    }
                }
            }
    }

    private fun buildCommentTree(allComments: List<Comment>): List<Comment> {
        val roots = mutableListOf<Comment>()
        val children = mutableMapOf<String, MutableList<Comment>>()

        allComments.forEach { comment ->
            if (comment.parentId == null) {
                roots.add(comment)
            } else {
                children.getOrPut(comment.parentId) { mutableListOf() }.add(comment)
            }
        }

        fun assemble(comment: Comment): Comment {
            val replies = children[comment.id]
                ?.map { assemble(it) }
                ?.sortedByDescending { it.timestamp } ?: emptyList()
            return comment.copy(replies = replies)
        }

        return roots.map { assemble(it) }.sortedByDescending { it.timestamp }
    }

    private fun observeUserNames() {
        groupSyncManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.RequestDelete -> _uiState.update { it.copy(noteToDelete = event.note) }
            NotesEvent.ConfirmDelete -> confirmDelete()
            is NotesEvent.ToggleTypeMenu -> _uiState.update { it.copy(showTypeMenu = event.show) }
            is NotesEvent.StartNewNote -> startNewNote(event.type)
            is NotesEvent.OpenNote -> openNote(event.note, event.editMode)
            is NotesEvent.UpdateDraftTitle -> _uiState.update { it.copy(draftTitle = event.title) }
            is NotesEvent.UpdateDraftContent -> _uiState.update { it.copy(draftContent = event.content) }
            is NotesEvent.UpdateChecklistInput -> _uiState.update { it.copy(checklistInput = event.text) }
            NotesEvent.AddChecklistItem -> addChecklistItem()
            is NotesEvent.ToggleChecklistItem -> toggleChecklistItem(event.id)
            is NotesEvent.RemoveChecklistItem -> removeChecklistItem(event.id)
            NotesEvent.SaveNote -> saveNote()
            NotesEvent.NavigateBack -> _uiState.update { it.copy(currentScreen = NotesScreenType.LIST, editingNote = null) }
            NotesEvent.ToggleEditMode -> _uiState.update { it.copy(isEditMode = !it.isEditMode) }
            
            is NotesEvent.AddComment -> addComment(event.content, event.parentId)
            is NotesEvent.EditComment -> editComment(event.commentId, event.newContent)
            is NotesEvent.DeleteComment -> deleteComment(event.commentId)
        }
    }

    private fun confirmDelete() {
        val note = _uiState.value.noteToDelete ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            getNotesColl(gid).document(note.id).delete().await()
            _uiState.update { it.copy(noteToDelete = null) }
        }
    }

    private fun startNewNote(type: NoteType) {
        val id = System.currentTimeMillis().toString()
        val newNote = NoteItem(
            id = id,
            type = type,
            lastUpdatedBy = settingsManager.getAppUserId(),
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { it.copy(
            currentScreen = NotesScreenType.EDIT,
            editingNote = newNote,
            creatingNoteType = type,
            isEditMode = true,
            showTypeMenu = false,
            draftTitle = "",
            draftContent = "",
            checklistInput = ""
        ) }
    }

    private fun openNote(note: NoteItem, editMode: Boolean) {
        _uiState.update { it.copy(
            currentScreen = NotesScreenType.EDIT,
            editingNote = note,
            creatingNoteType = null,
            isEditMode = editMode,
            draftTitle = note.title,
            draftContent = note.content,
            checklistInput = ""
        ) }
    }

    private fun addChecklistItem() {
        val text = _uiState.value.checklistInput.trim()
        val gid = settingsManager.getCurrentGroupId() ?: return
        if (text.isNotBlank()) {
            val id = System.currentTimeMillis().toString()
            val newItem = ChecklistItem(id = id, text = text, isChecked = false, timestamp = System.currentTimeMillis())
            val editingNote = _uiState.value.editingNote
            
            if (editingNote != null) {
                viewModelScope.launch {
                    val noteRef = getNotesColl(gid).document(editingNote.id)
                    noteRef.collection("checklist").document(id).set(newItem).await()
                    noteRef.set(mapOf("timestamp" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge()).await()
                    _uiState.update { it.copy(checklistInput = "") }
                }
            } else {
                _uiState.update { it.copy(checklistInput = "") }
            }
        }
    }

    private fun toggleChecklistItem(id: String) {
        val gid = settingsManager.getCurrentGroupId() ?: return
        val note = _uiState.value.editingNote ?: return
        val item = note.checklist.find { it.id == id } ?: return
        val updatedItem = item.copy(isChecked = !item.isChecked, timestamp = System.currentTimeMillis())
        viewModelScope.launch {
            val noteRef = getNotesColl(gid).document(note.id)
            noteRef.collection("checklist").document(id).set(updatedItem).await()
            noteRef.set(mapOf("timestamp" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    private fun removeChecklistItem(id: String) {
        val gid = settingsManager.getCurrentGroupId() ?: return
        val note = _uiState.value.editingNote ?: return
        viewModelScope.launch {
            val noteRef = getNotesColl(gid).document(note.id)
            noteRef.collection("checklist").document(id).delete().await()
            noteRef.set(mapOf("timestamp" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    private fun saveNote() {
        val state = _uiState.value
        val note = state.editingNote ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        
        val title = state.draftTitle.trim()
        
        if (title.isEmpty()) {
            _uiState.update { it.copy(error = Loc.notes_noteTitlePlaceholder) }
            return
        }
        
        val noteData = mapOf(
            "title" to title,
            "content" to state.draftContent.trim(),
            "type" to note.type.name,
            "timestamp" to System.currentTimeMillis(),
            "lastUpdatedBy" to settingsManager.getAppUserId()
        )
        
        viewModelScope.launch {
            getNotesColl(gid).document(note.id).set(noteData, com.google.firebase.firestore.SetOptions.merge()).await()
            _uiState.update { it.copy(isEditMode = false) }
        }
    }

    fun getAppUserId(): String = settingsManager.getAppUserId()
    fun getAuthUserId(): String = settingsManager.getUserId()

    // --- Comment Handling ---

    private fun addComment(content: String, parentId: String?) {
        val note = _uiState.value.editingNote ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        val id = "temp_${System.currentTimeMillis()}"
        val comment = Comment(
            id = id,
            content = content,
            userId = getAppUserId(),
            timestamp = System.currentTimeMillis(),
            parentId = parentId,
            isPending = true
        )

        // Optimistic UI Update
        _uiState.update { state ->
            val updatedNote = state.editingNote?.let { n ->
                if (parentId == null) {
                    n.copy(comments = (listOf(comment) + n.comments))
                } else {
                    // Update the parent comment's replies optimistically
                    fun updateReplies(comments: List<Comment>): List<Comment> = comments.map { c ->
                        if (c.id == parentId) {
                            c.copy(replies = (listOf(comment) + c.replies))
                        } else {
                            c.copy(replies = updateReplies(c.replies))
                        }
                    }
                    n.copy(comments = updateReplies(n.comments))
                }
            }
            state.copy(editingNote = updatedNote ?: state.editingNote)
        }

        viewModelScope.launch {
            try {
                val realId = System.currentTimeMillis().toString()
                val realComment = comment.copy(id = realId, isPending = false)
                val noteRef = getNotesColl(gid).document(note.id)
                noteRef.collection("comments").document(realId).set(realComment).await()
                noteRef.update("timestamp", System.currentTimeMillis()).await()
            } catch (e: Exception) {
                // Rollback optimistic update on failure
                // (In a full implementation we'd handle this more gracefully)
                android.util.Log.e("NotesVM", "Failed to add comment", e)
            }
        }
    }

    private fun editComment(commentId: String, newContent: String) {
        val note = _uiState.value.editingNote ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        
        // Find existing comment to preserve other properties
        fun findComment(comments: List<Comment>): Comment? {
            for (c in comments) {
                if (c.id == commentId) return c
                val found = findComment(c.replies)
                if (found != null) return found
            }
            return null
        }
        
        val existing = findComment(note.comments) ?: return
        val updatedComment = existing.copy(content = newContent, isEdited = true, timestamp = System.currentTimeMillis())

        viewModelScope.launch {
            val noteRef = getNotesColl(gid).document(note.id)
            noteRef.collection("comments").document(commentId).set(updatedComment).await()
            noteRef.set(mapOf("timestamp" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    private fun deleteComment(commentId: String) {
        val note = _uiState.value.editingNote ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return

        // Optimistic UI Update
        _uiState.update { state ->
            val updatedNote = state.editingNote?.let { n ->
                fun removeComment(list: List<Comment>): List<Comment> = list.filter { it.id != commentId }.map { 
                    it.copy(replies = removeComment(it.replies))
                }
                n.copy(comments = removeComment(n.comments))
            }
            state.copy(editingNote = updatedNote ?: state.editingNote)
        }

        viewModelScope.launch {
            try {
                val noteRef = getNotesColl(gid).document(note.id)
                noteRef.collection("comments").document(commentId).delete().await()
                noteRef.set(mapOf("timestamp" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (e: Exception) {
                android.util.Log.e("NotesVM", "Failed to delete comment", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notesListener?.remove()
    }
}
