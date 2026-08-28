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
import com.jtool.android.core.sync.helpers.CommentRepository
import com.jtool.android.core.components.comments.CommentController
import com.jtool.android.core.utils.SyncUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val commentController = CommentController(CommentRepository(), viewModelScope) { settingsManager.getAppUserId() }
    val comments = commentController.comments

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
                commentController.stop()
                val localNotes = manager.loadNotes(gid)
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
                        _uiState.update { it.copy(isLoading = true) }
                        val notesList = snapshot.documents.map { doc ->
                            async {
                                val note = doc.toObject(NoteItem::class.java)?.copy(id = doc.id)
                                if (note != null) {
                                    try {
                                        if (note.type == NoteType.CHECKLIST) {
                                            val clCountSnap = doc.reference.collection("checklist").count()
                                                .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                                            note.checklistCount = clCountSnap.count.toInt()
                                        }
                                        val cCountSnap = doc.reference.collection("comments").count()
                                            .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                                        note.commentCount = cCountSnap.count.toInt()
                                    } catch (e: Exception) { }
                                }
                                note
                            }
                        }.awaitAll().filterNotNull()

                        _uiState.update { state ->
                            val updatedEditingNote = state.editingNote?.let { editing ->
                                notesList.find { it.id == editing.id }?.let { fresh ->
                                    // PRESERVE only the checklist as it's separate
                                    fresh.copy(
                                        checklist = if (editing.checklist.isNotEmpty()) editing.checklist else fresh.checklist
                                    ).apply { 
                                        checklistCount = if (editing.checklist.isNotEmpty()) editing.checklist.size else fresh.checklistCount
                                        commentCount = fresh.commentCount
                                    }
                                }
                            }
                            state.copy(
                                notes = notesList,
                                editingNote = updatedEditingNote ?: state.editingNote,
                                isLoading = false
                            )
                        }
                        manager.saveNotes(notesList, gid)
                    }
                }
            }
    }

    private fun observeCommentsState() {
        combine(commentController.comments, commentController.isLoading) { comments, loading ->
            comments to loading
        }.onEach { (updatedComments, loading) ->
            _uiState.update { state ->
                val updatedNote = state.editingNote?.copy(comments = updatedComments)
                updatedNote?.apply { 
                    commentCount = updatedComments.size 
                }
                
                // Also update the note in the main list so the count updates immediately
                val updatedNotes = state.notes.map { 
                    if (it.id == state.editingNote?.id) {
                        it.copy().apply { 
                            commentCount = updatedComments.size
                            checklistCount = state.editingNote.checklist.size
                        }
                    } else it
                }
                
                state.copy(editingNote = updatedNote, notes = updatedNotes, isLoading = loading)
            }
        }.launchIn(viewModelScope)
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
            NotesEvent.NavigateBack -> {
                commentController.stop()
                _uiState.update { it.copy(currentScreen = NotesScreenType.LIST, editingNote = null) }
            }
            NotesEvent.ToggleEditMode -> _uiState.update { it.copy(isEditMode = !it.isEditMode) }
            
            is NotesEvent.AddComment -> commentController.addComment(event.content, event.parentId)
            is NotesEvent.EditComment -> commentController.editComment(event.commentId, event.newContent)
            is NotesEvent.DeleteComment -> commentController.deleteComment(event.commentId)
            NotesEvent.Refresh -> refresh()
            
            NotesEvent.ToggleSearch -> _uiState.update { it.copy(isSearchVisible = !it.isSearchVisible, searchQuery = "") }
            is NotesEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.query) }
            is NotesEvent.SetFilterType -> _uiState.update { it.copy(filterType = event.type) }
        }
    }

    private fun refresh() {
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            SyncUtils.performRefresh(
                onLoading = { loading -> _uiState.update { it.copy(isRefreshing = loading, isLoading = loading) } }
            ) {
                manager.saveNotes(emptyList(), gid) // Clear local cache
                _uiState.update { it.copy(notes = emptyList()) }
                
                val snapshot = getNotesColl(gid).orderBy("timestamp", Query.Direction.DESCENDING).get().await()
                val list = snapshot.documents.map { doc ->
                    async {
                        val note = doc.toObject(NoteItem::class.java)?.copy(id = doc.id)
                        if (note != null) {
                            try {
                                if (note.type == NoteType.CHECKLIST) {
                                    val clCountSnap = doc.reference.collection("checklist").count()
                                        .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                                    note.checklistCount = clCountSnap.count.toInt()
                                }
                                val cCountSnap = doc.reference.collection("comments").count()
                                    .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                                note.commentCount = cCountSnap.count.toInt()
                            } catch (e: Exception) { }
                        }
                        note
                    }
                }.awaitAll().filterNotNull()
                
                _uiState.update { it.copy(notes = list) }
                manager.saveNotes(list, gid)
            }
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
            checklistInput = "",
            isLoading = true
        ) }

        val gid = settingsManager.getCurrentGroupId() ?: return
        val noteRef = getNotesColl(gid).document(note.id)

        // Real-time Checklist Listener (Manual for now as it's separate from comments)
        viewModelScope.launch {
            try {
                val checklist = noteRef.collection("checklist")
                    .orderBy("timestamp").get().await()
                    .toObjects(ChecklistItem::class.java)
                
                _uiState.update { state ->
                    val updatedNote = state.editingNote?.let { 
                        if (it.id == note.id) it.copy(checklist = checklist) else it
                    }
                    state.copy(editingNote = updatedNote)
                }
            } catch (e: Exception) { }
        }

        // Real-time Comments via Unified Controller
        commentController.observe(noteRef.collection("comments"))
    }

    private fun addChecklistItem() {
        val text = _uiState.value.checklistInput.trim()
        val gid = settingsManager.getCurrentGroupId() ?: return
        if (text.isNotBlank()) {
            val id = System.currentTimeMillis().toString()
            val newItem = ChecklistItem(id = id, text = text, isChecked = false, timestamp = System.currentTimeMillis())
            val editingNote = _uiState.value.editingNote
            
            if (editingNote != null) {
                // Optimistic UI update
                _uiState.update { state ->
                    val updatedNote = state.editingNote?.copy(
                        checklist = state.editingNote.checklist + newItem
                    )
                    state.copy(editingNote = updatedNote, checklistInput = "", isLoading = true)
                }

                viewModelScope.launch {
                    try {
                        val noteRef = getNotesColl(gid).document(editingNote.id)
                        noteRef.collection("checklist").document(id).set(newItem).await()
                        noteRef.set(mapOf("timestamp" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge()).await()
                        _uiState.update { it.copy(isLoading = false) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
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
        
        // Optimistic UI update
        _uiState.update { state ->
            val updatedList = state.editingNote?.checklist?.map { if (it.id == id) updatedItem else it } ?: emptyList()
            state.copy(editingNote = state.editingNote?.copy(checklist = updatedList), isLoading = true)
        }

        viewModelScope.launch {
            try {
                val noteRef = getNotesColl(gid).document(note.id)
                noteRef.collection("checklist").document(id).set(updatedItem).await()
                noteRef.set(mapOf("timestamp" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge()).await()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun removeChecklistItem(id: String) {
        val gid = settingsManager.getCurrentGroupId() ?: return
        val note = _uiState.value.editingNote ?: return
        
        // Optimistic UI update
        _uiState.update { state ->
            val updatedList = state.editingNote?.checklist?.filter { it.id != id } ?: emptyList()
            state.copy(editingNote = state.editingNote?.copy(checklist = updatedList), isLoading = true)
        }

        viewModelScope.launch {
            try {
                val noteRef = getNotesColl(gid).document(note.id)
                noteRef.collection("checklist").document(id).delete().await()
                noteRef.set(mapOf("timestamp" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge()).await()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
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
        
        _uiState.update { it.copy(isLoading = true) }
        
        val noteData = mapOf(
            com.jtool.android.core.models.NoteItem::title.name to title,
            com.jtool.android.core.models.NoteItem::content.name to state.draftContent.trim(),
            com.jtool.android.core.models.NoteItem::type.name to note.type.name,
            com.jtool.android.core.models.NoteItem::timestamp.name to System.currentTimeMillis(),
            com.jtool.android.core.models.NoteItem::lastUpdatedBy.name to settingsManager.getAppUserId()
        )
        
        viewModelScope.launch {
            try {
                getNotesColl(gid).document(note.id).set(noteData, com.google.firebase.firestore.SetOptions.merge()).await()
                _uiState.update { it.copy(isEditMode = false, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun getAppUserId(): String = settingsManager.getAppUserId()
    fun getAuthUserId(): String = settingsManager.getUserId()

    override fun onCleared() {
        super.onCleared()
        notesListener?.remove()
        commentController.stop()
    }
}
