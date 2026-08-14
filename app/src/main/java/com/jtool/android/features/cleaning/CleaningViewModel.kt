package com.jtool.android.features.cleaning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.CleaningTask
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CleaningViewModel(
    private val manager: JCleaningManager,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleaningUiState())
    val uiState: StateFlow<CleaningUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeSyncData()
        startAutoResetTimer()
    }

    private fun loadInitialData() {
        val initialTasks = manager.loadTasks()
        val (updated, changed) = checkTasksForResets(initialTasks)
        if (changed) {
            updateTasks(updated)
        } else {
            _uiState.update { it.copy(tasks = updated) }
        }
    }

    private fun observeSyncData() {
        val currentGroupId = settingsManager.getCurrentGroupId()
        if (currentGroupId != null) {
            viewModelScope.launch {
                groupSyncManager.fetchUserSettings()
            }
            
            groupSyncManager.groupDataFlow(currentGroupId)
                .onEach { 
                    val syncedTasks = manager.loadTasks()
                    val (updated, changed) = checkTasksForResets(syncedTasks)
                    if (changed) {
                        updateTasks(updated)
                    } else {
                        _uiState.update { it.copy(tasks = updated) }
                    }
                }
                .launchIn(viewModelScope)

            groupSyncManager.userNamesFlow()
                .onEach { names ->
                    _uiState.update { it.copy(userNames = names) }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun startAutoResetTimer() {
        viewModelScope.launch {
            while (true) {
                delay(10000L)
                val currentTasks = manager.loadTasks()
                val (updated, changed) = checkTasksForResets(currentTasks)
                if (changed) {
                    updateTasks(updated)
                } else {
                    _uiState.update { it.copy(tasks = updated) }
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
            is CleaningEvent.ToggleDisabledView -> {
                _uiState.update { it.copy(showDisabledView = event.show) }
            }
            is CleaningEvent.ToggleAddDialog -> {
                _uiState.update { it.copy(showAddDialog = event.show) }
            }
            is CleaningEvent.RequestDelete -> {
                _uiState.update { it.copy(taskToDelete = event.task) }
            }
            CleaningEvent.ConfirmDelete -> {
                val task = _uiState.value.taskToDelete
                if (task != null) {
                    val newList = _uiState.value.tasks.filter { it.id != task.id }
                    updateTasks(newList)
                    _uiState.update { it.copy(taskToDelete = null) }
                }
            }
            is CleaningEvent.ToggleItem -> {
                val updated = _uiState.value.tasks.map {
                    if (it.id == event.task.id) {
                        it.copy(
                            isChecked = event.isChecked,
                            lastDoneTimestamp = if (event.isChecked) System.currentTimeMillis() else it.lastDoneTimestamp,
                            lastUpdatedBy = settingsManager.getAppUserId(),
                            timestamp = System.currentTimeMillis()
                        )
                    } else it
                }
                updateTasks(updated)
            }
            is CleaningEvent.UpdateTaskStatus -> {
                val updated = _uiState.value.tasks.map {
                    if (it.id == event.task.id) it.copy(isEnabled = event.isEnabled, isChecked = false) else it
                }
                updateTasks(updated)
            }
            is CleaningEvent.RequestEdit -> {
                _uiState.update { it.copy(editingTask = event.task) }
            }
            is CleaningEvent.SaveTask -> {
                val editing = _uiState.value.editingTask
                if (editing != null) {
                    val updated = _uiState.value.tasks.map {
                        if (it.id == editing.id) it.copy(
                            title = event.title,
                            frequencyDays = event.days,
                            isRecurring = event.isRecurring,
                            timestamp = System.currentTimeMillis()
                        ) else it
                    }
                    updateTasks(updated)
                    _uiState.update { it.copy(editingTask = null) }
                }
            }
            is CleaningEvent.AddTask -> {
                val newTask = CleaningTask(
                    id = System.currentTimeMillis(),
                    title = event.title.trim(),
                    frequencyDays = event.days,
                    isRecurring = event.isRecurring,
                    lastUpdatedBy = settingsManager.getAppUserId()
                )
                updateTasks(_uiState.value.tasks + newTask)
                _uiState.update { it.copy(showAddDialog = false) }
            }
        }
    }

    private fun updateTasks(newList: List<CleaningTask>) {
        _uiState.update { it.copy(tasks = newList) }
        manager.saveTasks(newList)
        triggerSync()
    }

    private fun triggerSync() {
        settingsManager.setSyncPending(true)
        viewModelScope.launch {
            groupSyncManager.pushGroupData()
        }
    }
}
