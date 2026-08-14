package com.jtool.android.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.NoteItem
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(
    private val manager: JNotesManager,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeSyncData()
    }

    private fun loadInitialData() {
        val notes = manager.loadNotes()
        _uiState.update { it.copy(notes = notes) }
    }

    private fun observeSyncData() {
        val currentGroupId = settingsManager.getCurrentGroupId()
        if (currentGroupId != null) {
            viewModelScope.launch {
                groupSyncManager.fetchUserSettings()
            }
            
            groupSyncManager.groupDataFlow(currentGroupId)
                .onEach { 
                    val notes = manager.loadNotes()
                    _uiState.update { it.copy(notes = notes) }
                }
                .launchIn(viewModelScope)

            groupSyncManager.userNamesFlow()
                .onEach { names ->
                    _uiState.update { it.copy(userNames = names) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.UpdateNewNoteText -> {
                _uiState.update { it.copy(newNoteText = event.text) }
            }
            is NotesEvent.RequestDelete -> {
                _uiState.update { it.copy(noteToDelete = event.note) }
            }
            NotesEvent.ConfirmDelete -> {
                val note = _uiState.value.noteToDelete
                if (note != null) {
                    val newList = _uiState.value.notes.filter { it.id != note.id }
                    updateNotes(newList)
                    _uiState.update { it.copy(noteToDelete = null) }
                }
            }
            NotesEvent.AddNote -> {
                val text = _uiState.value.newNoteText
                if (text.isNotBlank()) {
                    val newNote = NoteItem(
                        id = System.currentTimeMillis(),
                        content = text.trim(),
                        lastUpdatedBy = settingsManager.getAppUserId()
                    )
                    updateNotes(_uiState.value.notes + newNote)
                    _uiState.update { it.copy(newNoteText = "") }
                }
            }
        }
    }

    private fun updateNotes(newList: List<NoteItem>) {
        _uiState.update { it.copy(notes = newList) }
        manager.saveNotes(newList)
        triggerSync()
    }

    private fun triggerSync() {
        settingsManager.setSyncPending(true)
        viewModelScope.launch {
            groupSyncManager.pushGroupData()
        }
    }
}
