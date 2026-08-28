package com.jtool.android.features.notes

import android.content.Context
import com.jtool.android.core.models.NoteItem
import com.jtool.android.core.models.NoteType
import com.jtool.android.core.models.ChecklistItem
import com.jtool.android.core.models.Comment
import org.json.JSONArray
import org.json.JSONObject

class NotesManager(context: Context) {
    private val context = context
    private fun getPrefs(groupId: String?) = context.getSharedPreferences("jtool_notes_${groupId ?: "none"}", Context.MODE_PRIVATE)

    fun saveNotes(notes: List<NoteItem>, groupId: String?) {
        val root = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("type", note.type.name)
            obj.put("timestamp", note.timestamp)
            obj.put("lastUpdatedBy", note.lastUpdatedBy)
            obj.put("checklistCount", note.checklistCount)
            obj.put("commentCount", note.commentCount)
            
            val checklistArr = JSONArray()
            note.checklist.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("text", item.text)
                itemObj.put("isChecked", item.isChecked)
                itemObj.put("timestamp", item.timestamp)
                checklistArr.put(itemObj)
            }
            obj.put("checklist", checklistArr)
            
            obj.put("comments", serializeComments(note.comments))
            
            root.put(obj)
        }
        getPrefs(groupId).edit().putString("notes_json", root.toString()).apply()
    }

    fun clearCache(groupId: String?) {
        getPrefs(groupId).edit().clear().apply()
    }

    private fun serializeComments(comments: List<Comment>): JSONArray {
        val arr = JSONArray()
        comments.forEach { comment ->
            val obj = JSONObject()
            obj.put("id", comment.id)
            obj.put("content", comment.content)
            obj.put("userId", comment.userId)
            obj.put("timestamp", comment.timestamp)
            obj.put("isEdited", comment.isEdited)
            obj.put("parentId", comment.parentId)
            obj.put("replies", serializeComments(comment.replies))
            arr.put(obj)
        }
        return arr
    }

    fun loadNotes(groupId: String?): List<NoteItem> {
        val json = getPrefs(groupId).getString("notes_json", null)
        if (json != null) {
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
                    
                    val commentsArr = obj.optJSONArray("comments")
                    val comments = if (commentsArr != null) deserializeComments(commentsArr) else emptyList()
                    
                    list.add(NoteItem(
                        id = obj.getString("id"),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        type = NoteType.valueOf(obj.optString("type", NoteType.TEXT.name)),
                        checklist = checklist,
                        comments = comments,
                        timestamp = obj.getLong("timestamp"),
                        lastUpdatedBy = obj.getString("lastUpdatedBy")
                    ).apply {
                        checklistCount = obj.optInt("checklistCount", 0)
                        commentCount = obj.optInt("commentCount", 0)
                    })
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
        return emptyList()
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
