package com.jtool.android.features.links

import android.content.Context
import com.jtool.android.core.models.LinkItem

class LinksManager(context: Context) {
    private val context = context
    private fun getPrefs(groupId: String?) = context.getSharedPreferences("jtool_links_${groupId ?: "none"}", Context.MODE_PRIVATE)

    fun saveLinks(links: List<LinkItem>, groupId: String?) {
        val encoded = links.joinToString(";") { "${it.id}|${it.title.replace("|", "\\|").replace(";", "\\;")}|${it.url.replace("|", "\\|").replace(";", "\\;")}|${it.timestamp}|${it.lastUpdatedBy}" }
        getPrefs(groupId).edit().putString("links", encoded).apply()
    }

    fun loadLinks(groupId: String?): List<LinkItem> {
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
