package com.jtool.android.features.shopping

import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum

data class ShoppingUiState(
    val items: List<ShoppingItem> = emptyList(),
    val newItemText: String = "",
    val selectedAisle: ShoppingAisleEnum = ShoppingAisleEnum.COMMON,
    val showAislePicker: Boolean = false,
    val filterType: String = "All",
    val activeAisleFilters: Set<ShoppingAisleEnum> = emptySet(),
    val dropdownExpanded: Boolean = false,
    val itemToDelete: ShoppingItem? = null,
    val itemToRename: ShoppingItem? = null,
    val remoteSuggestions: List<String> = emptyList(),
    val userNames: Map<String, String> = emptyMap(),
    val selectedItem: ShoppingItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val activeAisles: List<ShoppingAisleEnum>
        get() = items.filter { !it.isChecked }.map { it.aisle }.distinct().sorted()

    val displayItems: List<ShoppingItem>
        get() = when {
            filterType == "Unactive" -> items.filter { it.isChecked }.sortedByDescending { it.timestamp }
            activeAisleFilters.isNotEmpty() -> {
                items.filter { !it.isChecked && activeAisleFilters.contains(it.aisle) }.sortedByDescending { it.id }
            }
            else -> items.filter { !it.isChecked }.sortedByDescending { it.id }
        }

    val checkedItems: List<ShoppingItem>
        get() = if (filterType == "All") {
            items.filter { it.isChecked }.sortedByDescending { it.timestamp }.take(5)
        } else {
            emptyList()
        }

    val suggestions: List<String>
        get() = if (newItemText.isNotBlank()) {
            (items.filter { it.isChecked && it.text.startsWith(newItemText, ignoreCase = true) }.map { it.text } +
             remoteSuggestions.filter { it.startsWith(newItemText, ignoreCase = true) })
                .distinct()
                .take(5)
        } else {
            emptyList()
        }
}
