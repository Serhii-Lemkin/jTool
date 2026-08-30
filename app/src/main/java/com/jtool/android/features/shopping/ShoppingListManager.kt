package com.jtool.android.features.shopping

import android.content.Context
import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum
import com.jtool.android.core.persistence.LocalPersistenceManager
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum

class ShoppingListManager(context: Context) : LocalPersistenceManager<ShoppingItem>(context, PersistencePrefixEnum.SHOPPING) {

    override fun save(items: List<ShoppingItem>, groupId: String?) {
        val encoded = items.joinToString(";") { "${it.id}|${it.text}|${it.isChecked}|${it.timestamp}|${it.lastUpdatedBy}|${it.aisle.name}" }
        getPrefs(groupId).edit().putString("items", encoded).apply()
    }

    override fun load(groupId: String?): List<ShoppingItem> {
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
                    aisle = if (parts.size >= 6) {
                        try { ShoppingAisleEnum.valueOf(parts[5]) } catch (e: Exception) { ShoppingAisleEnum.COMMON }
                    } else ShoppingAisleEnum.COMMON
                )
            } else null
        }
    }
}
