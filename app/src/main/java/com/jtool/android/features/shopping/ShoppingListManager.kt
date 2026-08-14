package com.jtool.android.features.shopping

import android.content.Context
import com.jtool.android.core.models.ShoppingItem

class ShoppingListManager(context: Context) {
    private val prefs = context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE)

    fun saveItems(items: List<ShoppingItem>) {
        val encoded = items.joinToString(";") { "${it.id}|${it.text}|${it.isChecked}|${it.timestamp}|${it.lastUpdatedBy}|${it.aisle}" }
        prefs.edit().putString("items", encoded).apply()
    }

    fun loadItems(): List<ShoppingItem> {
        val encoded = prefs.getString("items", "") ?: ""
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
