package com.jtool.android.features.links

import android.content.Context
import com.jtool.android.core.models.LinkItem

class LinksManager(context: Context) {
    private val prefs = context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE)

    fun saveLinks(links: List<LinkItem>) {
        val encoded = links.joinToString(";") { "${it.id}|${it.title.replace("|", "\\|").replace(";", "\\;")}|${it.url.replace("|", "\\|").replace(";", "\\;")}|${it.timestamp}|${it.lastUpdatedBy}" }
        prefs.edit().putString("links", encoded).apply()
    }

    fun loadLinks(): List<LinkItem> {
        val encoded = prefs.getString("links", "") ?: ""
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
