package com.jtool.android.core.backup

import android.content.Context
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum
import org.json.JSONObject

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

    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    fun createBackupJson(): String {
        val root = JSONObject()
        val shopPrefs = context.getSharedPreferences(PersistencePrefixEnum.SHOPPING.prefix, Context.MODE_PRIVATE)
        root.put("shopping_items", shopPrefs.getString("items", ""))
        val notesPrefs = context.getSharedPreferences(PersistencePrefixEnum.NOTES.prefix, Context.MODE_PRIVATE)
        root.put("notes_data", notesPrefs.getString("notes", ""))
        val linksPrefs = context.getSharedPreferences(PersistencePrefixEnum.LINKS.prefix, Context.MODE_PRIVATE)
        root.put("links_data", linksPrefs.getString("links", ""))
        val cleaningPrefs = context.getSharedPreferences(PersistencePrefixEnum.CLEANING.prefix, Context.MODE_PRIVATE)
        root.put("cleaning_tasks", cleaningPrefs.getString("tasks", ""))
        return root.toString(4)
    }

    fun restoreFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("shopping_items")) {
                val shopItems = root.getString("shopping_items")
                context.getSharedPreferences(PersistencePrefixEnum.SHOPPING.prefix, Context.MODE_PRIVATE)
                    .edit().putString("items", shopItems).apply()
            }
            if (root.has("notes_data")) {
                val notesData = root.getString("notes_data")
                context.getSharedPreferences(PersistencePrefixEnum.NOTES.prefix, Context.MODE_PRIVATE)
                    .edit().putString("notes", notesData).apply()
            }
            if (root.has("links_data")) {
                val linksData = root.getString("links_data")
                context.getSharedPreferences(PersistencePrefixEnum.LINKS.prefix, Context.MODE_PRIVATE)
                    .edit().putString("links", linksData).apply()
            }
            if (root.has("cleaning_tasks")) {
                val cleaningData = root.getString("cleaning_tasks")
                context.getSharedPreferences(PersistencePrefixEnum.CLEANING.prefix, Context.MODE_PRIVATE)
                    .edit().putString("tasks", cleaningData).apply()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
