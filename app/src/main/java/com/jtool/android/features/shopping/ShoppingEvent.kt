package com.jtool.android.features.shopping

import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum

sealed interface ShoppingEvent {
    data class UpdateNewItemText(val text: String) : ShoppingEvent
    data class ToggleAislePicker(val show: Boolean) : ShoppingEvent
    data class SelectAisle(val aisle: ShoppingAisleEnum) : ShoppingEvent
    data class UpdateFilter(val filter: String) : ShoppingEvent
    data class ToggleAisleFilter(val aisle: ShoppingAisleEnum) : ShoppingEvent
    object ClearAisleFilters : ShoppingEvent
    data class ToggleFilterDropdown(val expanded: Boolean) : ShoppingEvent
    data class RequestDelete(val item: ShoppingItem?) : ShoppingEvent
    object ConfirmDelete : ShoppingEvent
    data class RequestRename(val item: ShoppingItem?) : ShoppingEvent
    data class ConfirmRename(val newName: String) : ShoppingEvent
    object AddItem : ShoppingEvent
    data class ToggleItem(val item: ShoppingItem, val isChecked: Boolean) : ShoppingEvent
    data class RestoreItem(val text: String) : ShoppingEvent
    data class SelectItem(val item: ShoppingItem?) : ShoppingEvent
    object Refresh : ShoppingEvent
}
