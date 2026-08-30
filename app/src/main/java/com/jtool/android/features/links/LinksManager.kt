package com.jtool.android.features.links

import android.content.Context
import com.jtool.android.core.models.feature.links.LinkItem
import com.jtool.android.core.persistence.LocalPersistenceManager
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum

class LinksManager(context: Context) : LocalPersistenceManager<LinkItem>(context, PersistencePrefixEnum.LINKS) {

    override fun save(items: List<LinkItem>, groupId: String?) {
        val encoded = items.joinToString(";") { "${it.id}|${it.title.replace("|", "\\|").replace(";", "\\;")}|${it.url.replace("|", "\\|").replace(";", "\\;")}|${it.timestamp}|${it.lastUpdatedBy}" }
        getPrefs(groupId).edit().putString("links", encoded).apply()
    }

    override fun load(groupId: String?): List<LinkItem> {
        val encoded = getPrefs(groupId).getString("links", "") ?: ""
        if (encoded.isEmpty()) return emptyList()
        
        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 5) {
                LinkItem(
                    id = parts[0],
                    title = parts[1].replace("\\|", "|").replace("\\;", ";"),
                    url = parts[2].replace("\\|", "|").replace("\\;", ";"),
                    timestamp = parts[3].toLongOrNull() ?: 0L,
                    lastUpdatedBy = parts[4]
                )
            } else null
        }
    }
}
