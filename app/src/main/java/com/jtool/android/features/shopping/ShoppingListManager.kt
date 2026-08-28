package com.jtool.android.features.shopping

import android.content.Context
import com.jtool.android.core.models.ShoppingItem

class ShoppingListManager(context: Context) {
    private val context = context
    private fun getPrefs(groupId: String?) = context.getSharedPreferences("jtool_shopping_${groupId ?: "none"}", Context.MODE_PRIVATE)

    fun saveItems(items: List<ShoppingItem>, groupId: String?) {
        val encoded = items.joinToString(";") { "${it.id}|${it.text}|${it.isChecked}|${it.timestamp}|${it.lastUpdatedBy}|${it.aisle}" }
        getPrefs(groupId).edit().putString("items", encoded).apply()
    }

    fun loadItems(groupId: String?): List<ShoppingItem> {
        val encoded = getPrefs(groupId).getString("items", "") ?: ""
        if (encoded.isEmpty()) return emptyList()
        
        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 4) {
                ShoppingItem(
                    id = parts[0],
                    text = parts[1],
                    isChecked = parts[2].toBoolean(),
                    timestamp = parts[3].toLongOrNull() ?: 0L,
                    lastUpdatedBy = if (parts.size >= 5) parts[4] else "Unknown",
                    aisle = if (parts.size >= 6) parts[5] else "Common"
                )
            } else null
        }
    }
}
