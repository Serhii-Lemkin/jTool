package com.jtool.android.features.shopping

import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum

sealed interface ShoppingEvent {
    data class UpdateNewItemText(val text: String) : ShoppingEvent
    data class UpdateNewItemAmount(val amount: Double?) : ShoppingEvent
    data class UpdateNewItemUnit(val unit: String) : ShoppingEvent
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
    data class RequestEdit(val item: ShoppingItem?) : ShoppingEvent
    data class ConfirmEdit(
        val item: ShoppingItem,
        val newName: String,
        val newAmount: Double?,
        val newUnit: String,
        val newAisle: ShoppingAisleEnum
    ) : ShoppingEvent
    object AddItem : ShoppingEvent
    data class ToggleItem(val item: ShoppingItem, val isChecked: Boolean) : ShoppingEvent
    data class RestoreItem(val text: String) : ShoppingEvent
    data class SelectItem(val item: ShoppingItem?) : ShoppingEvent
    object Refresh : ShoppingEvent
}
