package com.jtool.android.features.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.sync.DataManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class TrackerViewModel(private val dataManager: DataManager) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private var groupId: String? = null
    private var recordsJob: Job? = null
    private var trackersJob: Job? = null

    fun setGroupId(id: String?) {
        if (groupId == id) return
        groupId = id
        observeTrackers()
    }

    private fun observeTrackers() {
        val gid = groupId ?: return
        trackersJob?.cancel()
        trackersJob = dataManager.getTrackersFlow(gid)
            .onEach { list -> 
                _uiState.update { it.copy(trackers = list) } 
                // Update current tracker if it's in the list to reflect any changes
                _uiState.value.currentTracker?.let { current ->
                    list.find { it.id == current.id }?.let { updated ->
                        _uiState.update { it.copy(currentTracker = updated) }
                    }
                }
            }
            .launchIn(viewModelScope)
            
        dataManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)
    }

    private fun observeRecords(trackerId: String) {
        recordsJob?.cancel()
        val gid = groupId ?: return
        recordsJob = dataManager.getTrackerRecordsFlow(gid, trackerId)
            .onEach { list -> 
                _uiState.update { it.copy(records = list) } 
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: TrackerEvent) {
        val gid = groupId ?: return
        when (event) {
            is TrackerEvent.CreateTracker -> {
                viewModelScope.launch {
                    dataManager.addTracker(gid, event.tracker)
                }
            }
            is TrackerEvent.UpdateTracker -> {
                viewModelScope.launch {
                    dataManager.updateTracker(gid, event.tracker)
                }
            }
            is TrackerEvent.UpdateUserColor -> {
                _uiState.value.currentTracker?.let { tracker ->
                    val updatedColors = tracker.userColors.toMutableMap().apply {
                        put(event.userId, event.color)
                    }
                    viewModelScope.launch {
                        dataManager.updateTracker(gid, tracker.copy(userColors = updatedColors))
                    }
                }
            }
            is TrackerEvent.DeleteTracker -> {
                viewModelScope.launch {
                    dataManager.deleteTracker(gid, event.trackerId)
                    if (_uiState.value.currentTracker?.id == event.trackerId) {
                        _uiState.update { it.copy(currentTracker = null, records = emptyList()) }
                    }
                }
            }
            is TrackerEvent.AddRecord -> {
                viewModelScope.launch {
                    dataManager.addTrackerRecord(gid, event.record)
                }
            }
            is TrackerEvent.UpdateRecord -> {
                viewModelScope.launch {
                    dataManager.updateTrackerRecord(gid, event.record)
                }
            }
            is TrackerEvent.DeleteRecord -> {
                viewModelScope.launch {
                    dataManager.deleteTrackerRecord(gid, event.recordId)
                }
            }
            is TrackerEvent.SelectTracker -> {
                _uiState.update { it.copy(currentTracker = event.tracker, records = emptyList()) }
                event.tracker?.let {
                    observeRecords(it.id)
                }
            }
            is TrackerEvent.SetTimeFilter -> {
                _uiState.update { it.copy(timeFilter = event.filter) }
            }
            TrackerEvent.LoadMore -> {
                _uiState.update { it.copy(pagingLimit = it.pagingLimit + 15) }
            }
            TrackerEvent.Refresh -> {
                _uiState.update { it.copy(isLoading = true, records = emptyList()) }
                viewModelScope.launch {
                    kotlinx.coroutines.delay(500.milliseconds)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
