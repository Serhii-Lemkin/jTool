package com.jtool.android.features.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.jtool.android.core.models.ShoppingItem
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.utils.SyncUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShoppingViewModel(
    private val manager: ShoppingListManager,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private var itemsListener: ListenerRegistration? = null

    init {
        observeGroupId()
        observeSyncData()
    }

    private fun observeGroupId() {
        groupSyncManager.currentGroupIdFlow()
            .onEach { gid ->
                // Clear current state to avoid data persisting from previous group
                _uiState.update { it.copy(items = emptyList()) }
                itemsListener?.remove()
                
                // Load from local (which may be empty if clearLocalCache was called)
                val localItems = manager.loadItems(gid)
                _uiState.update { it.copy(items = localItems) }
                
                if (gid != null) {
                    observeFirestoreItems(gid)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getShoppingColl(gid: String) = firestore.collection("groups").document(gid).collection("shopping")

    private fun observeFirestoreItems(gid: String) {
        itemsListener = getShoppingColl(gid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShoppingItem::class.java)?.copy(id = doc.id)
                }
                _uiState.update { it.copy(items = list) }
                manager.saveItems(list, gid)
            }
        }
    }

    private fun observeSyncData() {
        groupSyncManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)
            
        groupSyncManager.suggestionsFlow()
            .onEach { suggestions: List<String> -> 
                _uiState.update { it.copy(remoteSuggestions = suggestions) } 
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ShoppingEvent) {
        when (event) {
            is ShoppingEvent.UpdateNewItemText -> _uiState.update { it.copy(newItemText = event.text) }
            is ShoppingEvent.ToggleAislePicker -> _uiState.update { it.copy(showAislePicker = event.show) }
            is ShoppingEvent.SelectAisle -> selectAisle(event.aisle)
            is ShoppingEvent.UpdateFilter -> _uiState.update { it.copy(filterType = event.filter, dropdownExpanded = false) }
            is ShoppingEvent.ToggleFilterDropdown -> _uiState.update { it.copy(dropdownExpanded = event.expanded) }
            is ShoppingEvent.RequestDelete -> _uiState.update { it.copy(itemToDelete = event.item) }
            ShoppingEvent.ConfirmDelete -> confirmDelete()
            ShoppingEvent.AddItem -> addItem()
            is ShoppingEvent.ToggleItem -> toggleItem(event.item, event.isChecked)
            is ShoppingEvent.RestoreItem -> restoreItem(event.text)
            is ShoppingEvent.SelectItem -> _uiState.update { it.copy(selectedItem = event.item) }
            ShoppingEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            SyncUtils.performRefresh(
                onLoading = { loading -> _uiState.update { it.copy(isLoading = loading) } }
            ) {
                manager.saveItems(emptyList(), gid) // Clear local cache
                val snapshot = getShoppingColl(gid).get().await()
                val list = snapshot.documents.mapNotNull { it.toObject(ShoppingItem::class.java)?.copy(id = it.id) }
                _uiState.update { it.copy(items = list) }
                manager.saveItems(list, gid)
            }
        }
    }

    private fun selectAisle(aisle: String) {
        val selectedItem = _uiState.value.selectedItem
        if (selectedItem != null) {
            val gid = settingsManager.getCurrentGroupId() ?: return
            viewModelScope.launch {
                getShoppingColl(gid).document(selectedItem.id).update(com.jtool.android.core.models.ShoppingItem::aisle.name, aisle).await()
                _uiState.update { it.copy(selectedItem = null, showAislePicker = false) }
            }
        } else {
            _uiState.update { it.copy(selectedAisle = aisle, showAislePicker = false) }
        }
    }

    private fun confirmDelete() {
        val item = _uiState.value.itemToDelete ?: return
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            getShoppingColl(gid).document(item.id).delete().await()
            _uiState.update { it.copy(itemToDelete = null) }
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
                    _uiState.update { it.copy(newItemText = "", selectedAisle = "Common") }
                }
            }
        }
    }

    private fun toggleItem(item: ShoppingItem, isChecked: Boolean) {
        val gid = settingsManager.getCurrentGroupId() ?: return
        viewModelScope.launch {
            getShoppingColl(gid).document(item.id).update(
                com.jtool.android.core.models.ShoppingItem::isChecked.name, isChecked,
                com.jtool.android.core.models.ShoppingItem::timestamp.name, System.currentTimeMillis(),
                com.jtool.android.core.models.ShoppingItem::lastUpdatedBy.name, settingsManager.getAppUserId()
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

    override fun onCleared() {
        super.onCleared()
        itemsListener?.remove()
    }
}
