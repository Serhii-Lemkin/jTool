package com.jtool.android.features.notes

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.feature.notes.ChecklistItem
import com.jtool.android.core.models.feature.notes.NoteItem
import com.jtool.android.core.models.feature.notes.enums.NoteTypeEnum
import com.jtool.android.core.models.feature.notes.enums.NotesScreenTypeEnum
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.BaseSyncViewModel
import com.jtool.android.core.sync.helpers.FirestoreCommentRepository
import com.jtool.android.core.components.comments.CommentController
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotesViewModel(
    private val manager: NotesManager,
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : BaseSyncViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val commentController = CommentController(FirestoreCommentRepository(), viewModelScope) { settingsManager.getAppUserId() }
    val comments = commentController.comments

    init {
        observeGroupId()
        observeUserNames()
        observeCommentsState()
        
        isLoading.onEach { loading -> _uiState.update { it.copy(isLoading = loading) } }.launchIn(viewModelScope)
    }

    private fun observeGroupId() {
        dataManager.currentGroupIdFlow()
            .onEach { gid ->
                _uiState.update { it.copy(notes = emptyList(), editingNote = null) }
                clearListeners()
                commentController.stop()
                val localNotes = manager.load(gid)
                _uiState.update { it.copy(notes = localNotes) }
                if (gid != null) {
                    observeFirestoreNotes(gid)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getNotesColl(gid: String) = firestore.collection("groups").document(gid).collection("notes")

    private fun observeFirestoreNotes(gid: String) {
        val listener = getNotesColl(gid).orderBy(NoteItem::timestamp.name, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    cancelJobs()
                    val job = viewModelScope.launch {
                        _uiState.update { it.copy(isLoading = true) }
                        val notesList = snapshot.documents.map { doc ->
                            viewModelScope.async {
                                try {
                                    val note = doc.toObject(NoteItem::class.java)?.apply { id = doc.id }
                                    if (note != null) {
                                        if (note.type == NoteTypeEnum.CHECKLIST) {
                                            val clCountSnap = doc.reference.collection("checklist").count()
                                                .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                                            note.checklistCount = clCountSnap.count.toInt()
                                        }
                                        val cCountSnap = doc.reference.collection("comments").count()
                                            .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                                        note.commentCount = cCountSnap.count.toInt()
                                    }
                                    note
                                } catch (e: Exception) {
                                    Log.e("NotesVM", "Error deserializing note ${doc.id}", e)
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()

                        _uiState.update { state ->
                            val updatedEditingNote = state.editingNote?.let { editing ->
                                notesList.find { it.id == editing.id }?.let { fresh ->
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
                        manager.save(notesList, gid)
                    }
                    addJob(job)
                }
            }
        addListener(listener)
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
                
                val updatedNotes = state.notes.map { 
                    if (it.id == state.editingNote?.id) {
                        it.copy().apply { 
                            commentCount = updatedComments.size
                            checklistCount = state.editingNote.checklist.size
                        }
                    } else it
                }
                
                state.copy(
                    editingNote = updatedNote, 
                    notes = updatedNotes, 
                    isLoading = if (state.currentScreen == NotesScreenTypeEnum.EDIT) loading else state.isLoading
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeUserNames() {
        dataManager.userNamesFlow()
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
                _uiState.update { it.copy(currentScreen = NotesScreenTypeEnum.LIST, editingNote = null, isLoading = false) }
            }
            NotesEvent.ToggleEditMode -> _uiState.update { it.copy(isEditMode = !it.isEditMode) }
            is NotesEvent.AddComment -> commentController.addComment(event.content, event.parentId)
            is NotesEvent.EditComment -> commentController.editComment(event.commentId, event.newContent)
            is NotesEvent.DeleteComment -> commentController.deleteComment(event.commentId)
            NotesEvent.Refresh -> performRefresh {
                val gid = settingsManager.getCurrentGroupId() ?: return@performRefresh
                manager.save(emptyList(), gid)
                _uiState.update { it.copy(notes = emptyList()) }
                
                val snapshot = getNotesColl(gid).orderBy(NoteItem::timestamp.name, Query.Direction.DESCENDING)
                    .get(com.google.firebase.firestore.Source.SERVER).await()
                val list = snapshot.documents.map { doc ->
                    viewModelScope.async {
                        val note = doc.toObject(NoteItem::class.java)?.copy(id = doc.id)
                        if (note != null) {
                            try {
                                if (note.type == NoteTypeEnum.CHECKLIST) {
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
                manager.save(list, gid)
            }
            NotesEvent.ToggleSearch -> _uiState.update { it.copy(isSearchVisible = !it.isSearchVisible, searchQuery = "") }
            is NotesEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.query) }
            is NotesEvent.SetFilterType -> _uiState.update { it.copy(filterType = event.type) }
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

    private fun startNewNote(type: NoteTypeEnum) {
        val id = System.currentTimeMillis().toString()
        val newNote = NoteItem(
            id = id,
            type = type,
            lastUpdatedBy = settingsManager.getAppUserId(),
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { it.copy(
            currentScreen = NotesScreenTypeEnum.EDIT,
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
            currentScreen = NotesScreenTypeEnum.EDIT,
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

        viewModelScope.launch {
            try {
                val checklist = noteRef.collection("checklist")
                    .orderBy(ChecklistItem::timestamp.name).get().await()
                    .toObjects(ChecklistItem::class.java)
                
                _uiState.update { state ->
                    val updatedNote = state.editingNote?.let { 
                        if (it.id == note.id) it.copy(checklist = checklist) else it
                    }
                    state.copy(editingNote = updatedNote, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        commentController.observe(noteRef.collection("comments"))
    }

    private fun addChecklistItem() {
        val text = _uiState.value.checklistInput.trim()
        if (text.isNotBlank()) {
            val id = System.currentTimeMillis().toString()
            val newItem = ChecklistItem(id = id, text = text, isChecked = false, timestamp = System.currentTimeMillis())
            _uiState.update { state ->
                val editingNote = state.editingNote ?: return@update state
                val updatedChecklist = editingNote.checklist + newItem
                val updatedNote = editingNote.copy(checklist = updatedChecklist)
                state.copy(editingNote = updatedNote, checklistInput = "")
            }
        }
    }

    private fun toggleChecklistItem(id: String) {
        _uiState.update { state ->
            val editingNote = state.editingNote ?: return@update state
            val updatedChecklist = editingNote.checklist.map { 
                if (it.id == id) it.copy(isChecked = !it.isChecked, timestamp = System.currentTimeMillis()) else it 
            }
            val updatedNote = editingNote.copy(checklist = updatedChecklist)
            state.copy(editingNote = updatedNote)
        }
    }

    private fun removeChecklistItem(id: String) {
        _uiState.update { state ->
            val editingNote = state.editingNote ?: return@update state
            val updatedChecklist = editingNote.checklist.filter { it.id != id }
            val updatedNote = editingNote.copy(checklist = updatedChecklist)
            state.copy(editingNote = updatedNote)
        }
    }

    private fun saveNote() {
        val state = _uiState.value
        val note = state.editingNote ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        
        val title = state.draftTitle.trim()
        val content = state.draftContent.trim()
        val now = System.currentTimeMillis()
        val userId = settingsManager.getAppUserId()
        
        _uiState.update { it.copy(isLoading = true) }
        
        val noteData = mapOf(
            NoteItem::title.name to title,
            NoteItem::content.name to content,
            NoteItem::type.name to note.type.name,
            NoteItem::timestamp.name to now,
            NoteItem::lastUpdatedBy.name to userId
        )
        
        viewModelScope.launch {
            try {
                val noteRef = getNotesColl(gid).document(note.id)
                noteRef.set(noteData, SetOptions.merge()).await()

                if (note.type == NoteTypeEnum.CHECKLIST) {
                    val checklistColl = noteRef.collection("checklist")
                    val existingDocs = checklistColl.get().await()
                    val batch = firestore.batch()
                    
                    val currentIds = note.checklist.map { it.id }.toSet()
                    existingDocs.documents.forEach { doc ->
                        if (!currentIds.contains(doc.id)) {
                            batch.delete(doc.reference)
                        }
                    }
                    
                    note.checklist.forEach { item ->
                        val itemRef = checklistColl.document(item.id)
                        batch.set(itemRef, item)
                    }
                    batch.commit().await()
                }

                val updatedNote = note.copy(
                    title = title,
                    content = content,
                    timestamp = now,
                    lastUpdatedBy = userId
                )

                _uiState.update { st ->
                    val updatedNotes = st.notes.map { if (it.id == updatedNote.id) updatedNote else it }
                    st.copy(
                        notes = if (updatedNotes.any { it.id == updatedNote.id }) updatedNotes else listOf(updatedNote) + updatedNotes,
                        editingNote = updatedNote,
                        isEditMode = false,
                        isLoading = false
                    )
                }
                manager.save(_uiState.value.notes, gid)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun getAppUserId(): String = settingsManager.getAppUserId()
    fun getAuthUserId(): String = settingsManager.getUserId()

    override fun onCleared() {
        super.onCleared()
        commentController.stop()
    }
}
