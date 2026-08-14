package com.jtool.android.core.sync.helpers

import com.jtool.android.core.models.ChecklistItem
import com.jtool.android.core.models.CleaningTask
import com.jtool.android.core.models.Comment
import com.jtool.android.core.models.LinkItem
import com.jtool.android.core.models.NoteItem
import com.jtool.android.core.models.NoteType
import com.jtool.android.core.models.ShoppingItem
import org.json.JSONArray

object DataParser {

    fun parseShopping(encoded: String): List<ShoppingItem> {
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 4) {
                ShoppingItem(
                    id = parts[0],
                    text = parts[1],
                    isChecked = parts[2].toBoolean(),
                    timestamp = parts[3].toLongOrNull() ?: 0L,
                    lastUpdatedBy = if (parts.size >= 5) parts[4] else "",
                    aisle = if (parts.size >= 6) parts[5] else "Common"
                )
            } else null
        }
    }

    fun parseLinks(encoded: String): List<LinkItem> {
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 3) {
                LinkItem(
                    id = parts[0],
                    title = parts[1].replace("\\|", "|").replace("\\;", ";"),
                    url = parts[2].replace("\\|", "|").replace("\\;", ";"),
                    timestamp = if (parts.size >= 4) parts[3].toLongOrNull() ?: 0L else 0L,
                    lastUpdatedBy = if (parts.size >= 5) parts[4] else ""
                )
            } else null
        }
    }

    fun parseCleaning(encoded: String): List<CleaningTask> {
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 8) {
                CleaningTask(
                    id = parts[0],
                    title = parts[1].replace("\\|", "|").replace("\\;", ";"),
                    frequencyDays = parts[2].toIntOrNull() ?: 1,
                    isRecurring = parts[3].toBoolean(),
                    lastDoneTimestamp = parts[4].toLongOrNull() ?: 0L,
                    isChecked = parts[5].toBoolean(),
                    isEnabled = parts[6].toBoolean(),
                    lastUpdatedBy = parts[7],
                    timestamp = if (parts.size >= 9) parts[8].toLongOrNull() ?: 0L else 0L
                )
            } else null
        }
    }

    fun parseNotes(json: String): List<NoteItem> {
        if (json.isEmpty()) return emptyList()
        return try {
            val root = JSONArray(json)
            val list = mutableListOf<NoteItem>()
            for (i in 0 until root.length()) {
                val obj = root.getJSONObject(i)
                val checklist = mutableListOf<ChecklistItem>()
                val checklistArr = obj.optJSONArray("checklist")
                if (checklistArr != null) {
                    for (j in 0 until checklistArr.length()) {
                        val itemObj = checklistArr.getJSONObject(j)
                        checklist.add(ChecklistItem(
                            id = itemObj.getString("id"),
                            text = itemObj.getString("text"),
                            isChecked = itemObj.getBoolean("isChecked"),
                            timestamp = itemObj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                }
                
                list.add(NoteItem(
                    id = obj.getString("id"),
                    title = obj.optString("title", ""),
                    content = obj.optString("content", ""),
                    type = NoteType.valueOf(obj.optString("type", NoteType.TEXT.name)),
                    checklist = checklist,
                    comments = deserializeComments(obj.optJSONArray("comments") ?: JSONArray()),
                    timestamp = obj.getLong("timestamp"),
                    lastUpdatedBy = obj.getString("lastUpdatedBy")
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun deserializeComments(arr: JSONArray): List<Comment> {
        val list = mutableListOf<Comment>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val repliesArr = obj.optJSONArray("replies")
            val replies = if (repliesArr != null) deserializeComments(repliesArr) else emptyList()
            
            list.add(Comment(
                id = obj.getString("id"),
                content = obj.getString("content"),
                userId = obj.getString("userId"),
                timestamp = obj.getLong("timestamp"),
                isEdited = obj.optBoolean("isEdited", false),
                parentId = if (obj.has("parentId")) obj.getString("parentId") else null,
                replies = replies
            ))
        }
        return list
    }
}
