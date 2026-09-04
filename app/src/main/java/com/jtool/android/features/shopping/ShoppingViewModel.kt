package com.jtool.android.features.shopping

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.BaseSyncViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShoppingViewModel(
    private val manager: ShoppingListManager,
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager
) : BaseSyncViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    init {
        observeGroupId()
        observeSyncData()
        
        isLoading.onEach { loading -> _uiState.update { it.copy(isLoading = loading) } }.launchIn(viewModelScope)
    }

    private fun observeGroupId() {
        dataManager.currentGroupIdFlow()
            .onEach { gid ->
                _uiState.update { it.copy(items = emptyList()) }
                clearListeners()
                
                val localItems = manager.load(gid)
                _uiState.update { it.copy(items = localItems) }
                
                if (gid != null) {
                    observeFirestoreItems(gid)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getShoppingColl(gid: String) = firestore.collection("groups").document(gid).collection("shopping")

    private fun observeFirestoreItems(gid: String) {
        val listener = getShoppingColl(gid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ShoppingItem::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        android.util.Log.e("ShoppingVM", "Error deserializing item ${doc.id}", e)
                        null
                    }
                }
                _uiState.update { it.copy(items = list) }
                manager.save(list, gid)
            }
        }
        addListener(listener)
    }

    private fun observeSyncData() {
        dataManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ShoppingEvent) {
        when (event) {
            is ShoppingEvent.UpdateNewItemText -> _uiState.update { it.copy(newItemText = event.text) }
            is ShoppingEvent.ToggleAislePicker -> _uiState.update { it.copy(showAislePicker = event.show) }
            is ShoppingEvent.SelectAisle -> selectAisle(event.aisle)
            is ShoppingEvent.UpdateFilter -> _uiState.update { it.copy(filterType = event.filter, activeAisleFilters = emptySet(), dropdownExpanded = false) }
            is ShoppingEvent.ToggleAisleFilter -> toggleAisleFilter(event.aisle)
            ShoppingEvent.ClearAisleFilters -> _uiState.update { it.copy(activeAisleFilters = emptySet(), filterType = "All") }
            is ShoppingEvent.ToggleFilterDropdown -> _uiState.update { it.copy(dropdownExpanded = event.expanded) }
            is ShoppingEvent.RequestDelete -> _uiState.update { it.copy(itemToDelete = event.item) }
            ShoppingEvent.ConfirmDelete -> confirmDelete()
            is ShoppingEvent.RequestRename -> _uiState.update { it.copy(itemToRename = event.item) }
            is ShoppingEvent.ConfirmRename -> confirmRename(event.newName)
            ShoppingEvent.AddItem -> addItem()
            is ShoppingEvent.ToggleItem -> toggleItem(event.item, event.isChecked)
            is ShoppingEvent.RestoreItem -> restoreItem(event.text)
            is ShoppingEvent.SelectItem -> _uiState.update { it.copy(selectedItem = event.item) }
            ShoppingEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        val gid = settingsManager.getCurrentGroupId() ?: return
        performRefresh(
            onRefreshStart = {
                manager.save(emptyList(), gid)
                _uiState.update { it.copy(items = emptyList()) }
            }
        ) {
            val snapshot = getShoppingColl(gid).get(com.google.firebase.firestore.Source.SERVER).await()
            val list = snapshot.documents.mapNotNull { it.toObject(ShoppingItem::class.java)?.copy(id = it.id) }
            _uiState.update { it.copy(items = list) }
            manager.save(list, gid)
        }
    }

    private fun selectAisle(aisle: ShoppingAisleEnum) {
        val selectedItem = _uiState.value.selectedItem
        if (selectedItem != null) {
            val gid = settingsManager.getCurrentGroupId() ?: return
            viewModelScope.launch {
                getShoppingColl(gid).document(selectedItem.id).update(ShoppingItem::aisle.name, aisle).await()
                _uiState.update { it.copy(selectedItem = null, showAislePicker = false) }
            }
        } else {
            _uiState.update { it.copy(selectedAisle = aisle, showAislePicker = false) }
        }
    }

    private fun toggleAisleFilter(aisle: ShoppingAisleEnum) {
        val current = _uiState.value.activeAisleFilters
        val next = if (current.contains(aisle)) current - aisle else current + aisle
        _uiState.update { it.copy(activeAisleFilters = next, filterType = "Filtered") }
    }

    private fun confirmDelete() {
        val item = _uiState.value.itemToDelete ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            getShoppingColl(gid).document(item.id).delete().await()
            _uiState.update { it.copy(itemToDelete = null) }
        }
    }

    private fun confirmRename(newName: String) {
        val item = _uiState.value.itemToRename ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        if (newName.isNotBlank()) {
            viewModelScope.launch {
                getShoppingColl(gid).document(item.id).update(ShoppingItem::text.name, newName).await()
                _uiState.update { it.copy(itemToRename = null) }
            }
        }
    }

    private fun addItem() {
        val text = _uiState.value.newItemText.trim()
        val gid = settingsManager.getCurrentGroupId() ?: return
        if (text.isNotBlank()) {
            val existing = _uiState.value.items.find { it.text.equals(text, ignoreCase = true) }
            if (existing != null) {
                if (existing.isChecked) toggleItem(existing, false)
                _uiState.update { it.copy(newItemText = "") }
            } else {
                val id = System.currentTimeMillis().toString()
                val newItem = ShoppingItem(
                    id = id,
                    text = text,
                    isChecked = false,
                    lastUpdatedBy = settingsManager.getAppUserId(),
                    aisle = _uiState.value.selectedAisle,
                    timestamp = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    getShoppingColl(gid).document(id).set(newItem).await()
                    _uiState.update { it.copy(newItemText = "", selectedAisle = ShoppingAisleEnum.COMMON) }
                }
            }
        }
    }

    private fun toggleItem(item: ShoppingItem, isChecked: Boolean) {
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            getShoppingColl(gid).document(item.id).update(
                ShoppingItem::isChecked.name, isChecked,
                ShoppingItem::timestamp.name, System.currentTimeMillis(),
                ShoppingItem::lastUpdatedBy.name, settingsManager.getAppUserId()
            ).await()
        }
    }

    private fun restoreItem(text: String) {
        val existing = _uiState.value.items.find { it.text.equals(text, ignoreCase = true) }
        val gid = settingsManager.getCurrentGroupId() ?: return
        if (existing != null) {
            toggleItem(existing, false)
        } else {
            val id = System.currentTimeMillis().toString()
            val newItem = ShoppingItem(
                id = id,
                text = text,
                isChecked = false,
                lastUpdatedBy = settingsManager.getAppUserId(),
                aisle = _uiState.value.selectedAisle,
                timestamp = System.currentTimeMillis()
            )
            viewModelScope.launch {
                getShoppingColl(gid).document(id).set(newItem).await()
            }
        }
        _uiState.update { it.copy(newItemText = "") }
    }
}
