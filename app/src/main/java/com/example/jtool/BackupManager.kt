package com.example.jtool

import android.content.Context
import org.json.JSONObject
import java.io.File

class BackupManager(private val context: Context) {

    fun getCurrentVersionCode(): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) it.longVersionCode.toInt() else it.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }
    
    fun createBackupJson(): String {
        val root = JSONObject()
        val shopPrefs = context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE)
        root.put("shopping_items", shopPrefs.getString("items", ""))
        val notesPrefs = context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE)
        root.put("notes_data", notesPrefs.getString("notes", ""))
        val linksPrefs = context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE)
        root.put("links_data", linksPrefs.getString("links", ""))
        val cleaningPrefs = context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE)
        root.put("cleaning_tasks", cleaningPrefs.getString("tasks", ""))
        return root.toString(4)
    }

    fun restoreFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("shopping_items")) {
                val shopItems = root.getString("shopping_items")
                context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE)
                    .edit().putString("items", shopItems).apply()
            }
            if (root.has("notes_data")) {
                val notesData = root.getString("notes_data")
                context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE)
                    .edit().putString("notes", notesData).apply()
            }
            if (root.has("links_data")) {
                val linksData = root.getString("links_data")
                context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE)
                    .edit().putString("links", linksData).apply()
            }
            if (root.has("cleaning_tasks")) {
                val cleaningData = root.getString("cleaning_tasks")
                context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE)
                    .edit().putString("tasks", cleaningData).apply()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
