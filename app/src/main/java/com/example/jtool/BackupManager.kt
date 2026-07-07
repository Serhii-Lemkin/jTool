package com.example.jtool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class BackupManager(private val context: Context) {

    private val SHOP_FILENAME = "jtool_shop_list.json"
    private val NOTES_FILENAME = "jtool_notes.json"
    private val LINKS_FILENAME = "jtool_links.json"
    private val CLEANING_FILENAME = "jtool_cleaning_planner.json"
    private val VERSION_FILENAME = "version.txt"
    private val APK_FILENAME = "jtool_update.apk"

    private fun pushFile(folderUriString: String?, filename: String, content: String): Boolean {
        if (folderUriString.isNullOrEmpty()) return false
        return try {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
            
            var file = folder.findFile(filename)
            if (file == null) {
                file = folder.createFile("application/json", filename)
            }
            
            context.contentResolver.openOutputStream(file!!.uri, "rwt")?.use { stream ->
                stream.write(content.toByteArray())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun pullFile(folderUriString: String?, filename: String): String? {
        if (folderUriString.isNullOrEmpty()) return null
        return try {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null
            val file = folder.findFile(filename) ?: return null
            
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    // Shopping List Sync
    fun syncPushShopList(folderUriString: String?): Boolean {
        val json = JSONObject().apply {
            val prefs = context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE)
            put("shopping_items", prefs.getString("items", ""))
        }.toString(4)
        return pushFile(folderUriString, SHOP_FILENAME, json)
    }

    fun syncPullShopList(folderUriString: String?): Boolean {
        val content = pullFile(folderUriString, SHOP_FILENAME) ?: return false
        return try {
            val root = JSONObject(content)
            if (root.has("shopping_items")) {
                val shopItems = root.getString("shopping_items")
                context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE)
                    .edit().putString("items", shopItems).apply()
                true
            } else false
        } catch (_: Exception) { false }
    }

    // Notes Sync
    fun syncPushNotes(folderUriString: String?): Boolean {
        val json = JSONObject().apply {
            val prefs = context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE)
            put("notes_data", prefs.getString("notes", ""))
        }.toString(4)
        return pushFile(folderUriString, NOTES_FILENAME, json)
    }

    fun syncPullNotes(folderUriString: String?): Boolean {
        val content = pullFile(folderUriString, NOTES_FILENAME) ?: return false
        return try {
            val root = JSONObject(content)
            if (root.has("notes_data")) {
                val notesData = root.getString("notes_data")
                context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE)
                    .edit().putString("notes", notesData).apply()
                true
            } else false
        } catch (_: Exception) { false }
    }

    // Links Sync
    fun syncPushLinks(folderUriString: String?): Boolean {
        val json = JSONObject().apply {
            val prefs = context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE)
            put("links_data", prefs.getString("links", ""))
        }.toString(4)
        return pushFile(folderUriString, LINKS_FILENAME, json)
    }

    fun syncPullLinks(folderUriString: String?): Boolean {
        val content = pullFile(folderUriString, LINKS_FILENAME) ?: return false
        return try {
            val root = JSONObject(content)
            if (root.has("links_data")) {
                val linksData = root.getString("links_data")
                context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE)
                    .edit().putString("links", linksData).apply()
                true
            } else false
        } catch (_: Exception) { false }
    }

    // Cleaning Sync
    fun syncPushCleaning(folderUriString: String?): Boolean {
        val json = JSONObject().apply {
            val prefs = context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE)
            put("cleaning_tasks", prefs.getString("tasks", ""))
        }.toString(4)
        return pushFile(folderUriString, CLEANING_FILENAME, json)
    }

    fun syncPullCleaning(folderUriString: String?): Boolean {
        val content = pullFile(folderUriString, CLEANING_FILENAME) ?: return false
        return try {
            val root = JSONObject(content)
            if (root.has("cleaning_tasks")) {
                val tasks = root.getString("cleaning_tasks")
                context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE)
                    .edit().putString("tasks", tasks).apply()
                true
            } else false
        } catch (_: Exception) { false }
    }

    fun syncPush(folderUriString: String?): Boolean {
        val s1 = syncPushShopList(folderUriString)
        val s2 = syncPushNotes(folderUriString)
        val s3 = syncPushLinks(folderUriString)
        val s4 = syncPushCleaning(folderUriString)
        return s1 && s2 && s3 && s4
    }

    fun syncPull(folderUriString: String?): Boolean {
        syncPullShopList(folderUriString)
        syncPullNotes(folderUriString)
        syncPullLinks(folderUriString)
        syncPullCleaning(folderUriString)
        return true
    }

    fun getCurrentVersionCode(): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) it.longVersionCode.toInt() else it.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getAvailableVersion(folderUriString: String?): Int? = withContext(Dispatchers.IO) {
        if (folderUriString.isNullOrEmpty()) return@withContext null
        return@withContext try {
            val folderUri = Uri.parse(folderUriString)
            
            // Aggressively force a refresh of the folder content listing
            try {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    folderUri,
                    DocumentsContract.getTreeDocumentId(folderUri)
                )
                context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) { }
                }
            } catch (e: Exception) {
                android.util.Log.w("jToolSync", "Failed to force refresh folder", e)
            }

            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext null
            val file = folder.findFile(VERSION_FILENAME) ?: return@withContext null
            
            val versionStr = context.contentResolver.openInputStream(file.uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readLine()
            }?.trim()
            
            android.util.Log.d("jToolSync", "Checking version.txt: $versionStr")
            versionStr?.toIntOrNull()
        } catch (e: Exception) {
            android.util.Log.e("jToolSync", "Error checking version", e)
            null
        }
    }

    suspend fun checkForUpdate(folderUriString: String?): Int? {
        val onlineVersion = getAvailableVersion(folderUriString) ?: return null
        val currentVersion = getCurrentVersionCode()
        return if (onlineVersion > currentVersion) onlineVersion else null
    }

    fun downloadAndInstallUpdate(folderUriString: String?) {
        if (folderUriString.isNullOrEmpty()) return
        try {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return
            val apkFile = folder.findFile(APK_FILENAME) ?: return

            val localFile = File(context.externalCacheDir, APK_FILENAME)
            context.contentResolver.openInputStream(apkFile.uri)?.use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", localFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
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
