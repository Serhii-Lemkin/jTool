package com.jtool.android.features.shopping

import com.jtool.android.core.models.ShoppingItem

data class ShoppingUiState(
    val items: List<ShoppingItem> = emptyList(),
    val newItemText: String = "",
    val selectedAisle: String = "Common",
    val showAislePicker: Boolean = false,
    val filterType: String = "All",
    val dropdownExpanded: Boolean = false,
    val itemToDelete: ShoppingItem? = null,
    val remoteSuggestions: List<String> = emptyList(),
    val userNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val activeAisles: List<String>
        get() = items.filter { !it.isChecked }.map { it.aisle }.distinct().sorted()

    val displayItems: List<ShoppingItem>
        get() = when {
            filterType == "Unactive" -> items.filter { it.isChecked }.sortedByDescending { it.timestamp }
            filterType.startsWith("Aisle:") -> {
                val aisleName = filterType.removePrefix("Aisle:")
                items.filter { !it.isChecked && it.aisle == aisleName }.sortedByDescending { it.id }
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
