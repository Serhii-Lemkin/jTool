package com.jtool.android.features.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.ShoppingItem
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShoppingViewModel(
    private val manager: ShoppingListManager,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeSyncData()
    }

    private fun loadInitialData() {
        val items = manager.loadItems()
        _uiState.update { it.copy(items = items) }
    }

    private fun observeSyncData() {
        val currentGroupId = settingsManager.getCurrentGroupId()
        if (currentGroupId != null) {
            viewModelScope.launch {
                groupSyncManager.fetchUserSettings()
            }
            
            groupSyncManager.groupDataFlow(currentGroupId)
                .onEach { 
                    val items = manager.loadItems()
                    _uiState.update { it.copy(items = items) }
                }
                .launchIn(viewModelScope)

            groupSyncManager.suggestionsFlow()
                .onEach { suggestions ->
                    _uiState.update { it.copy(remoteSuggestions = suggestions) }
                }
                .launchIn(viewModelScope)

            groupSyncManager.userNamesFlow()
                .onEach { names ->
                    _uiState.update { it.copy(userNames = names) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onEvent(event: ShoppingEvent) {
        when (event) {
            is ShoppingEvent.UpdateNewItemText -> {
                _uiState.update { it.copy(newItemText = event.text) }
            }
            is ShoppingEvent.ToggleAislePicker -> {
                _uiState.update { it.copy(showAislePicker = event.show) }
            }
            is ShoppingEvent.SelectAisle -> {
                _uiState.update { it.copy(selectedAisle = event.aisle, showAislePicker = false) }
            }
            is ShoppingEvent.UpdateFilter -> {
                _uiState.update { it.copy(filterType = event.filter, dropdownExpanded = false) }
            }
            is ShoppingEvent.ToggleFilterDropdown -> {
                _uiState.update { it.copy(dropdownExpanded = event.expanded) }
            }
            is ShoppingEvent.RequestDelete -> {
                _uiState.update { it.copy(itemToDelete = event.item) }
            }
            ShoppingEvent.ConfirmDelete -> {
                val item = _uiState.value.itemToDelete
                if (item != null) {
                    val newList = _uiState.value.items.filter { it.id != item.id }
                    updateItems(newList)
                    _uiState.update { it.copy(itemToDelete = null) }
                }
            }
            ShoppingEvent.AddItem -> {
                val text = _uiState.value.newItemText
                if (text.isNotBlank()) {
                    val trimmed = text.trim()
                    val existing = _uiState.value.items.find { it.text.equals(trimmed, ignoreCase = true) }
                    if (existing != null) {
                        if (existing.isChecked) {
                            val updated = _uiState.value.items.map {
                                if (it.id == existing.id) it.copy(isChecked = false, timestamp = System.currentTimeMillis()) else it
                            }
                            updateItems(updated)
                            _uiState.update { it.copy(newItemText = "") }
                        }
                    } else {
                        val newItem = ShoppingItem(
                            id = System.currentTimeMillis(),
                            text = trimmed,
                            isChecked = false,
                            lastUpdatedBy = settingsManager.getAppUserId(),
                            aisle = _uiState.value.selectedAisle
                        )
                        updateItems(_uiState.value.items + newItem)
                        _uiState.update { it.copy(newItemText = "", selectedAisle = "Common") }
                    }
                }
            }
            is ShoppingEvent.ToggleItem -> {
                val updated = _uiState.value.items.map {
                    if (it.id == event.item.id) it.copy(
                        isChecked = event.isChecked,
                        timestamp = System.currentTimeMillis(),
                        lastUpdatedBy = settingsManager.getAppUserId()
                    ) else it
                }
                updateItems(updated)
            }
            is ShoppingEvent.RestoreItem -> {
                val existing = _uiState.value.items.find { it.text.equals(event.text, ignoreCase = true) }
                val updated = if (existing != null) {
                    _uiState.value.items.map {
                        if (it.id == existing.id) it.copy(
                            isChecked = false,
                            timestamp = System.currentTimeMillis(),
                            lastUpdatedBy = settingsManager.getAppUserId()
                        ) else it
                    }
                } else {
                    val newItem = ShoppingItem(
                        id = System.currentTimeMillis(),
                        text = event.text,
                        isChecked = false,
                        lastUpdatedBy = settingsManager.getAppUserId(),
                        aisle = _uiState.value.selectedAisle
                    )
                    _uiState.value.items + newItem
                }
                updateItems(updated)
                _uiState.update { it.copy(newItemText = "") }
            }
        }
    }

    private fun updateItems(newList: List<ShoppingItem>) {
        _uiState.update { it.copy(items = newList) }
        manager.saveItems(newList)
        triggerSync()
    }

    private fun triggerSync() {
        settingsManager.setSyncPending(true)
        viewModelScope.launch {
            groupSyncManager.pushGroupData()
        }
    }
}
