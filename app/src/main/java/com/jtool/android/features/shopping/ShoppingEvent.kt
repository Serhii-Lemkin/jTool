package com.jtool.android.features.shopping

import com.jtool.android.core.models.ShoppingItem

sealed interface ShoppingEvent {
    data class UpdateNewItemText(val text: String) : ShoppingEvent
    data class ToggleAislePicker(val show: Boolean) : ShoppingEvent
    data class SelectAisle(val aisle: String) : ShoppingEvent
    data class UpdateFilter(val filter: String) : ShoppingEvent
    data class ToggleFilterDropdown(val expanded: Boolean) : ShoppingEvent
    data class RequestDelete(val item: ShoppingItem?) : ShoppingEvent
    object ConfirmDelete : ShoppingEvent
    object AddItem : ShoppingEvent
    data class ToggleItem(val item: ShoppingItem, val isChecked: Boolean) : ShoppingEvent
    data class RestoreItem(val text: String) : ShoppingEvent
    data class SelectItem(val item: ShoppingItem?) : ShoppingEvent
    object Refresh : ShoppingEvent
}
