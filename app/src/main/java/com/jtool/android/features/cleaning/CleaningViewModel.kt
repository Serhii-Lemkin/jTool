package com.jtool.android.features.cleaning

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.models.feature.cleaning.CleaningTask
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.BaseSyncViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CleaningViewModel(
    private val manager: CleaningManager,
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : BaseSyncViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(CleaningUiState())
    val uiState: StateFlow<CleaningUiState> = _uiState.asStateFlow()

    init {
        observeGroupId()
        observeSyncData()
        startAutoResetTimer()
        
        isLoading.onEach { loading -> _uiState.update { it.copy(isLoading = loading) } }.launchIn(viewModelScope)
    }

    private fun observeGroupId() {
        dataManager.currentGroupIdFlow()
            .onEach { gid ->
                _uiState.update { it.copy(tasks = emptyList()) }
                clearListeners()
                val localTasks = manager.load(gid)
                _uiState.update { it.copy(tasks = localTasks) }
                if (gid != null) {
                    observeFirestoreTasks(gid)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getCleaningColl(gid: String) = firestore.collection("groups").document(gid).collection("cleaning")

    private fun observeFirestoreTasks(gid: String) {
        val listener = getCleaningColl(gid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(CleaningTask::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        Log.e("CleaningVM", "Error deserializing task ${doc.id}", e)
                        null
                    }
                }
                val (updated, changed) = checkTasksForResets(list)
                if (changed) {
                    updated.forEach { task -> saveTaskToFirestore(task) }
                }
                _uiState.update { it.copy(tasks = updated) }
                manager.save(updated, gid)
            }
        }
        addListener(listener)
    }

    private fun observeSyncData() {
        dataManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)
    }

    private fun startAutoResetTimer() {
        viewModelScope.launch {
            while (true) {
                delay(60000L)
                val (updated, changed) = checkTasksForResets(_uiState.value.tasks)
                if (changed) {
                    updated.forEach { task -> saveTaskToFirestore(task) }
                }
            }
        }
    }

    private fun checkTasksForResets(currentTasks: List<CleaningTask>): Pair<List<CleaningTask>, Boolean> {
        val today = LocalDate.now()
        var anyChanged = false
        val updated = currentTasks.map { task ->
            if (task.isEnabled && task.isChecked && task.isRecurring && task.lastDoneTimestamp > 0) {
                val doneDate = Instant.ofEpochMilli(task.lastDoneTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val diffDays = ChronoUnit.DAYS.between(doneDate, today)

                if (diffDays >= task.frequencyDays) {
                    anyChanged = true
                    task.copy(isChecked = false)
                } else task
            } else task
        }
        return updated to anyChanged
    }

    fun onEvent(event: CleaningEvent) {
        when (event) {
            is CleaningEvent.ToggleDisabledView -> _uiState.update { it.copy(showDisabledView = event.show) }
            is CleaningEvent.ToggleAddDialog -> _uiState.update { it.copy(showAddDialog = event.show) }
            is CleaningEvent.RequestDelete -> _uiState.update { it.copy(taskToDelete = event.task) }
            CleaningEvent.ConfirmDelete -> confirmDelete()
            is CleaningEvent.ToggleItem -> toggleItem(event.task, event.isChecked)
            is CleaningEvent.UpdateTaskStatus -> updateTaskStatus(event.task, event.isEnabled)
            is CleaningEvent.RequestEdit -> _uiState.update { it.copy(editingTask = event.task) }
            is CleaningEvent.SaveTask -> saveEditedTask(event.title, event.days, event.isRecurring)
            is CleaningEvent.AddTask -> addTask(event.title, event.days, event.isRecurring)
            CleaningEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        val gid = settingsManager.getCurrentGroupId() ?: return
        performRefresh(
            onRefreshStart = {
                _uiState.update { it.copy(tasks = emptyList()) }
                manager.save(emptyList(), gid)
            }
        ) {
            val snapshot = getCleaningColl(gid).get(com.google.firebase.firestore.Source.SERVER).await()
            val list = snapshot.documents.mapNotNull { it.toObject(CleaningTask::class.java)?.copy(id = it.id) }
            _uiState.update { it.copy(tasks = list) }
            manager.save(list, gid)
        }
    }

    private fun confirmDelete() {
        val task = _uiState.value.taskToDelete ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            getCleaningColl(gid).document(task.id).delete().await()
            _uiState.update { it.copy(taskToDelete = null) }
        }
    }

    private fun toggleItem(task: CleaningTask, isChecked: Boolean) {
        val updated = task.copy(
            isChecked = isChecked,
            lastDoneTimestamp = if (isChecked) System.currentTimeMillis() else task.lastDoneTimestamp,
            lastUpdatedBy = settingsManager.getAppUserId(),
            timestamp = System.currentTimeMillis()
        )
        saveTaskToFirestore(updated)
    }

    private fun updateTaskStatus(task: CleaningTask, isEnabled: Boolean) {
        val updated = task.copy(isEnabled = isEnabled, isChecked = false, timestamp = System.currentTimeMillis())
        saveTaskToFirestore(updated)
    }

    private fun saveEditedTask(title: String, days: Int, isRecurring: Boolean) {
        val editing = _uiState.value.editingTask ?: return
        val updated = editing.copy(
            title = title,
            frequencyDays = days,
            isRecurring = isRecurring,
            timestamp = System.currentTimeMillis(),
            lastUpdatedBy = settingsManager.getAppUserId()
        )
        saveTaskToFirestore(updated)
        _uiState.update { it.copy(editingTask = null) }
    }

    private fun addTask(title: String, days: Int, isRecurring: Boolean) {
        val id = System.currentTimeMillis().toString()
        val newTask = CleaningTask(
            id = id,
            title = title.trim(),
            frequencyDays = days,
            isRecurring = isRecurring,
            lastUpdatedBy = settingsManager.getAppUserId(),
            timestamp = System.currentTimeMillis()
        )
        saveTaskToFirestore(newTask)
        _uiState.update { it.copy(showAddDialog = false) }
    }

    private fun saveTaskToFirestore(task: CleaningTask) {
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            getCleaningColl(gid).document(task.id).set(task).await()
        }
    }
}
