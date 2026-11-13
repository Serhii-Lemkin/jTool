package com.jtool.android.features.vaults.common

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GoogleDrivePermissionsHelper {

    suspend fun getVaultPermissionsWithEmails(
        context: Context,
        vaultDriveFolderId: String
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val (token, _) = GoogleDriveVaultHelper.fetchAccessToken(context)
        if (token.isNullOrEmpty() || vaultDriveFolderId.isEmpty()) return@withContext emptyMap()

        val map = mutableMapOf<String, Boolean>()
        try {
            val url = URL("https://www.googleapis.com/drive/v3/files/$vaultDriveFolderId/permissions?fields=permissions(id,emailAddress)")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")

            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val permissionsArray = json.optJSONArray("permissions")
                if (permissionsArray != null) {
                    for (i in 0 until permissionsArray.length()) {
                        val perm = permissionsArray.getJSONObject(i)
                        val email = perm.optString("emailAddress")
                        if (email.isNotEmpty()) {
                            map[email.lowercase()] = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DriveVault", "Error getting permissions", e)
        }
        map
    }

    suspend fun grantPermissionToUser(
        context: Context,
        vaultDriveFolderId: String,
        email: String
    ): Boolean = withContext(Dispatchers.IO) {
        val (token, _) = GoogleDriveVaultHelper.fetchAccessToken(context)
        if (token.isNullOrEmpty() || vaultDriveFolderId.isEmpty() || email.isEmpty()) return@withContext false
        grantDrivePermission(token, vaultDriveFolderId, email)
    }

    suspend fun grantDrivePermission(token: String, fileOrFolderId: String, email: String): Boolean = withContext(Dispatchers.IO) {
        if (fileOrFolderId.isEmpty() || email.isBlank() || fileOrFolderId.startsWith("jtools_") || fileOrFolderId.startsWith("local_")) return@withContext false
        try {
            val url = URL("https://www.googleapis.com/drive/v3/files/$fileOrFolderId/permissions?sendNotificationEmail=false")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("role", "writer")
                put("type", "user")
                put("emailAddress", email.trim())
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.e("DriveVault", "grantDrivePermission exception for $fileOrFolderId", e)
            false
        }
    }

    suspend fun removeAnyonePermissions(token: String, fileOrFolderId: String): Boolean = withContext(Dispatchers.IO) {
        if (fileOrFolderId.isEmpty() || fileOrFolderId.startsWith("jtools_") || fileOrFolderId.startsWith("local_")) return@withContext false
        try {
            val url = URL("https://www.googleapis.com/drive/v3/files/$fileOrFolderId/permissions?fields=permissions(id,type)")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val permissionsArray = json.optJSONArray("permissions")
                if (permissionsArray != null) {
                    for (i in 0 until permissionsArray.length()) {
                        val perm = permissionsArray.getJSONObject(i)
                        if (perm.optString("type") == "anyone") {
                            val permId = perm.optString("id")
                            val delUrl = URL("https://www.googleapis.com/drive/v3/files/$fileOrFolderId/permissions/$permId")
                            val delConn = delUrl.openConnection() as HttpURLConnection
                            delConn.requestMethod = "DELETE"
                            delConn.setRequestProperty("Authorization", "Bearer $token")
                            delConn.responseCode
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("DriveVault", "removeAnyonePermissions error for $fileOrFolderId", e)
            false
        }
    }

    suspend fun syncGroupFolderPermissions(
        context: Context,
        vaultDriveFolderId: String,
        groupMemberEmails: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val (token, _) = GoogleDriveVaultHelper.fetchAccessToken(context)
        if (token.isNullOrEmpty() || vaultDriveFolderId.isEmpty()) return@withContext false

        try {
            val url = URL("https://www.googleapis.com/drive/v3/files/$vaultDriveFolderId/permissions?fields=permissions(id,emailAddress)")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")

            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val permissionsObj = JSONObject(response)
                val permissionsArray = permissionsObj.optJSONArray("permissions")

                val currentSharedEmails = mutableListOf<String>()
                if (permissionsArray != null) {
                    for (i in 0 until permissionsArray.length()) {
                        val perm = permissionsArray.getJSONObject(i)
                        val email = perm.optString("emailAddress")
                        if (email.isNotEmpty()) {
                            currentSharedEmails.add(email)
                        }
                    }
                }

                val needsUpdate = groupMemberEmails.any { !currentSharedEmails.contains(it) } ||
                    currentSharedEmails.any { !groupMemberEmails.contains(it) && !it.contains("google.com") }

                if (needsUpdate) {
                    removeAnyonePermissions(token, vaultDriveFolderId)
                    groupMemberEmails.forEach { email ->
                        grantDrivePermission(token, vaultDriveFolderId, email)
                    }
                    return@withContext true
                }
            } else {
                Log.e("DriveVault", "Failed to get permissions for $vaultDriveFolderId. Code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("DriveVault", "Error syncing permissions", e)
        }
        false
    }
}
