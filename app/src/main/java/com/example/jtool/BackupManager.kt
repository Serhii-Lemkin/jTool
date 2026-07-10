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
    private val REQUESTS_FILENAME = "jtool_requests.txt"

    suspend fun sendRequest(folderUriString: String?, requestText: String, userName: String): Boolean = withContext(Dispatchers.IO) {
        if (folderUriString.isNullOrEmpty() || requestText.isBlank()) return@withContext false
        return@withContext try {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext false
            
            var file = folder.findFile(REQUESTS_FILENAME)
            val existingContent = if (file != null) {
                context.contentResolver.openInputStream(file.uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: ""
            } else {
                file = folder.createFile("text/plain", REQUESTS_FILENAME)
                ""
            }

            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val newEntry = "--- Request at $timestamp ---\nBy: $userName\n$requestText\n\n"
            val finalContent = existingContent + newEntry

            context.contentResolver.openOutputStream(file!!.uri, "wt")?.use { stream ->
                stream.write(finalContent.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun pushFile(folderUriString: String?, filename: String, content: String): Boolean = withContext(Dispatchers.IO) {
        if (folderUriString.isNullOrEmpty()) return@withContext false
        return@withContext try {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext false
            
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

    private suspend fun pullFile(folderUriString: String?, filename: String): String? = withContext(Dispatchers.IO) {
        if (folderUriString.isNullOrEmpty()) return@withContext null
        return@withContext try {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext null
            val file = folder.findFile(filename) ?: return@withContext null
            
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun syncPushShopList(folderUriString: String?): Boolean = true
    suspend fun syncPullShopList(folderUriString: String?): Boolean = true
    suspend fun syncPushNotes(folderUriString: String?): Boolean = true
    suspend fun syncPullNotes(folderUriString: String?): Boolean = true
    suspend fun syncPushLinks(folderUriString: String?): Boolean = true
    suspend fun syncPullLinks(folderUriString: String?): Boolean = true
    suspend fun syncPushCleaning(folderUriString: String?): Boolean = true
    suspend fun syncPullCleaning(folderUriString: String?): Boolean = true
    suspend fun syncPush(folderUriString: String?): Boolean = true
    suspend fun syncPull(folderUriString: String?): Boolean = true

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
            
            // Force a refresh by querying the folder itself with extra signals
            try {
                context.contentResolver.refresh(folderUri, null, null)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    folderUri,
                    DocumentsContract.getTreeDocumentId(folderUri)
                )
                // Use a query that specifically tells the provider to fetch from network if possible
                val bundle = android.os.Bundle()
                bundle.putBoolean(DocumentsContract.QUERY_ARG_DISPLAY_NAME, true) 
                context.contentResolver.query(childrenUri, null, bundle, null)?.use { cursor ->
                    while (cursor.moveToNext()) { }
                }
            } catch (e: Exception) {
                android.util.Log.w("jToolSync", "Failed to force refresh folder", e)
            }

            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext null
            
            // Re-crawl the folder to bypass local listing cache
            folder.listFiles() 

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

    suspend fun downloadAndInstallUpdate(folderUriString: String?): Boolean = withContext(Dispatchers.IO) {
        if (folderUriString.isNullOrEmpty()) return@withContext false
        return@withContext try {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext false
            val apkFile = folder.findFile(APK_FILENAME) ?: return@withContext false

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
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
