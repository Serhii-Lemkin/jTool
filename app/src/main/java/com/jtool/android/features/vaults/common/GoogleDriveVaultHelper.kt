package com.jtool.android.features.vaults.common

import android.accounts.Account
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object GoogleDriveVaultHelper {

    private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"

    fun buildDriveFolderUrl(folderId: String): String {
        if (folderId.isEmpty() || folderId.startsWith("jtools_")) return "https://drive.google.com/drive/my-drive"
        return "https://drive.google.com/drive/folders/$folderId"
    }

    fun buildDriveFileUrl(fileId: String): String {
        if (fileId.isEmpty() || fileId.startsWith("jtools_")) return "https://drive.google.com/drive/my-drive"
        return "https://drive.google.com/file/d/$fileId/view"
    }

    private fun parseApiErrorMessage(rawErrorBody: String?): String {
        if (rawErrorBody.isNullOrBlank()) return "Google Drive API Error"
        return try {
            val json = JSONObject(rawErrorBody)
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                if (errObj != null && errObj.has("message")) {
                    errObj.getString("message")
                } else {
                    json.optString("error_description", rawErrorBody)
                }
            } else {
                rawErrorBody
            }
        } catch (_: Exception) {
            rawErrorBody
        }
    }

    suspend fun checkDrivePermissions(context: Context): Pair<Boolean, String?> {
        val (token, errorMsg) = getAccessToken(context)
        if (token != null) {
            return Pair(true, null)
        }
        return Pair(false, errorMsg)
    }

    private suspend fun getAccessToken(context: Context): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.e("DriveVault", "getAccessToken: GoogleSignIn account is NULL.")
                return@withContext Pair(null, "Not signed in with Google")
            }

            val gAccount = account.account ?: account.email?.let { Account(it, "com.google") }
            if (gAccount == null) {
                Log.e("DriveVault", "getAccessToken: Account object and email are NULL.")
                return@withContext Pair(null, "Google account email is missing")
            }

            Log.d("DriveVault", "getAccessToken: Fetching token for ${gAccount.name}...")
            val token = GoogleAuthUtil.getToken(context, gAccount, DRIVE_SCOPE)
            Log.d("DriveVault", "getAccessToken: Successfully obtained OAuth token!")
            Pair(token, null)
        } catch (e: UserRecoverableAuthException) {
            Log.e("DriveVault", "getAccessToken: User consent required for Drive scope", e)
            try {
                e.intent?.let { intent ->
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            } catch (launchEx: Exception) {
                Log.e("DriveVault", "Failed to launch consent intent", launchEx)
            }
            Pair(null, "Google Drive consent prompt opened. Please grant permission and tap retry.")
        } catch (e: GoogleAuthException) {
            Log.e("DriveVault", "getAccessToken: GoogleAuthException: ${e.message}", e)
            Pair(null, "Google Auth Error: ${e.message}")
        } catch (e: Exception) {
            Log.e("DriveVault", "getAccessToken: Exception: ${e.javaClass.simpleName} - ${e.message}", e)
            Pair(null, "Auth Error: ${e.message}")
        }
    }

    suspend fun getVaultPermissionsWithEmails(
        context: Context,
        vaultDriveFolderId: String
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val (token, _) = getAccessToken(context)
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
        return@withContext map
    }

    suspend fun grantPermissionToUser(
        context: Context,
        vaultDriveFolderId: String,
        email: String
    ): Boolean = withContext(Dispatchers.IO) {
        val (token, _) = getAccessToken(context)
        if (token.isNullOrEmpty() || vaultDriveFolderId.isEmpty() || email.isEmpty()) return@withContext false
        return@withContext grantDrivePermission(token, vaultDriveFolderId, email)
    }

    private suspend fun grantDrivePermission(token: String, fileOrFolderId: String, email: String): Boolean = withContext(Dispatchers.IO) {
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
            Log.d("DriveVault", "grantDrivePermission code $code for file $fileOrFolderId (email: $email)")
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

    suspend fun renameRemoteDriveItem(context: Context, driveId: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        if (driveId.isEmpty() || driveId.startsWith("jtools_") || driveId.startsWith("local_")) return@withContext false
        try {
            val (token, _) = getAccessToken(context)
            if (token.isNullOrEmpty()) return@withContext false

            val url = URL("https://www.googleapis.com/drive/v3/files/$driveId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true

            val jsonBody = JSONObject().apply { put("name", newName) }
            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            Log.d("DriveVault", "renameRemoteDriveItem response code $responseCode for '$driveId'")
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e("DriveVault", "Failed to rename remote Drive item $driveId", e)
            false
        }
    }

    suspend fun deleteRemoteDriveItem(context: Context, driveId: String): Boolean = withContext(Dispatchers.IO) {
        if (driveId.isEmpty() || driveId.startsWith("jtools_") || driveId.startsWith("local_")) return@withContext false
        try {
            val (token, _) = getAccessToken(context)
            if (token.isNullOrEmpty()) return@withContext false

            Log.d("DriveVault", "deleteRemoteDriveItem: Deleting remote Drive item '$driveId'...")
            val url = URL("https://www.googleapis.com/drive/v3/files/$driveId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = conn.responseCode
            Log.d("DriveVault", "deleteRemoteDriveItem response code $responseCode for '$driveId'")
            responseCode in 200..299 || responseCode == 404
        } catch (e: Exception) {
            Log.e("DriveVault", "Failed to delete remote Drive item $driveId", e)
            false
        }
    }

    private suspend fun createRemoteDriveFolder(token: String, folderName: String, parentId: String? = null): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            Log.d("DriveVault", "createRemoteDriveFolder: Creating '$folderName' (parentId: $parentId)...")
            val url = URL("https://www.googleapis.com/drive/v3/files")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
                if (parentId != null) {
                    put("parents", org.json.JSONArray().put(parentId))
                }
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            Log.d("DriveVault", "createRemoteDriveFolder: Response code $responseCode for '$folderName'")

            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val folderId = json.optString("id")
                Log.d("DriveVault", "createRemoteDriveFolder: Success '$folderName' -> ID: $folderId")
                Pair(folderId, null)
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val cleanMsg = parseApiErrorMessage(errorBody)
                Log.e("DriveVault", "createRemoteDriveFolder: API Error ($responseCode): $cleanMsg")
                Pair(null, cleanMsg)
            }
        } catch (e: Exception) {
            Log.e("DriveVault", "createRemoteDriveFolder: Exception for '$folderName'", e)
            Pair(null, "Network/API Exception: ${e.message}")
        }
    }

    private suspend fun uploadRemoteDriveFile(
        token: String, 
        parentFolderId: String, 
        fileName: String, 
        sourceFile: File,
        onProgress: ((Long) -> Unit)? = null
    ): Pair<Pair<String, String>?, String?> = withContext(Dispatchers.IO) {
        try {
            Log.d("DriveVault", "uploadRemoteDriveFile: Uploading '$fileName' to parent '$parentFolderId'...")
            val boundary = "-----jToolsBoundary" + System.currentTimeMillis()
            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            conn.setChunkedStreamingMode(64 * 1024)
            conn.doOutput = true

            val metadataJson = JSONObject().apply {
                put("name", fileName)
                if (parentFolderId.isNotEmpty() && !parentFolderId.startsWith("jtools_")) {
                    put("parents", org.json.JSONArray().put(parentFolderId))
                }
            }

            conn.outputStream.use { os ->
                val writer = os.bufferedWriter()
                writer.write("--$boundary\r\n")
                writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                writer.write(metadataJson.toString())
                writer.write("\r\n--$boundary\r\n")
                writer.write("Content-Type: application/octet-stream\r\n\r\n")
                writer.flush()

                sourceFile.inputStream().use { isStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesCopied = 0L
                    var lastReportTime = 0L
                    while (true) {
                        val bytes = isStream.read(buffer)
                        if (bytes < 0) break
                        os.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        
                        if (onProgress != null) {
                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 500) {
                                onProgress(bytesCopied)
                                lastReportTime = now
                            }
                        }
                    }
                }

                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            val responseCode = conn.responseCode
            Log.d("DriveVault", "uploadRemoteDriveFile: Response code $responseCode for '$fileName'")

            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val fileId = json.optString("id")
                Log.d("DriveVault", "uploadRemoteDriveFile: Success '$fileName' -> File ID: $fileId")
                if (fileId.isNotEmpty()) {
                    Pair(Pair(fileId, buildDriveFileUrl(fileId)), null)
                } else Pair(null, "Empty file ID returned")
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val cleanMsg = parseApiErrorMessage(errorBody)
                Log.e("DriveVault", "uploadRemoteDriveFile: API Error ($responseCode): $cleanMsg")
                Pair(null, cleanMsg)
            }
        } catch (e: Exception) {
            Log.e("DriveVault", "uploadRemoteDriveFile: Exception for '$fileName'", e)
            Pair(null, "Upload Exception: ${e.message}")
        }
    }

    suspend fun verifyDriveFolderExists(token: String, folderId: String): Boolean = withContext(Dispatchers.IO) {
        if (folderId.isEmpty() || folderId.startsWith("jtools_") || folderId.startsWith("local_")) return@withContext false
        try {
            val url = URL("https://www.googleapis.com/drive/v3/files/$folderId?fields=id,trashed")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")

            val code = conn.responseCode
            if (code in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val trashed = json.optBoolean("trashed", false)
                return@withContext !trashed
            }
            false
        } catch (e: Exception) {
            Log.e("DriveVault", "verifyDriveFolderExists error for $folderId", e)
            false
        }
    }

    private suspend fun createReadmeInFolder(token: String, folderId: String) = withContext(Dispatchers.IO) {
        try {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val readmeContent = """
                ===================================================================
                jTools App - Google Drive Storage Information
                ===================================================================
                Folder Created: $dateStr

                Purpose:
                This folder is automatically managed by jTools to organize your shared 
                vault documents and application data securely inside your Google Drive.

                Important Notice:
                - Do NOT delete this folder.
                - If this folder or its contents are deleted from Google Drive, 
                  previously uploaded files will no longer be accessible within the app.
                - If deleted, jTools will automatically recreate a fresh folder structure 
                  when needed, but past files must be restored from Google Drive trash.
                ===================================================================
            """.trimIndent()

            val boundary = "-----jToolsBoundary" + System.currentTimeMillis()
            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            conn.doOutput = true

            val metadataJson = JSONObject().apply {
                put("name", "README.txt")
                if (folderId.isNotEmpty()) {
                    put("parents", org.json.JSONArray().put(folderId))
                }
            }

            conn.outputStream.use { os ->
                val writer = os.bufferedWriter()
                writer.write("--$boundary\r\n")
                writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                writer.write(metadataJson.toString())
                writer.write("\r\n--$boundary\r\n")
                writer.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
                writer.write(readmeContent)
                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }
            conn.responseCode
        } catch (e: Exception) {
            Log.e("DriveVault", "Failed to create README.txt", e)
        }
    }

    suspend fun createVaultDriveFolder(
        context: Context,
        vaultName: String,
        isPrivate: Boolean,
        groupMemberEmails: List<String>,
        dataManager: com.jtool.android.core.sync.DataManager,
        userId: String
    ): Pair<Pair<String, String>?, String?> {
        if (vaultName.isBlank()) return Pair(null, "Vault name is blank")

        val (token, tokenError) = getAccessToken(context)
        if (token == null) {
            Log.e("DriveVault", "createVaultDriveFolder: Aborting, token error: $tokenError")
            return Pair(null, tokenError ?: "Not authenticated with Google")
        }

        var rootFolderId = dataManager.getJtoolsFolderId()
        if (rootFolderId.isNotEmpty() && !verifyDriveFolderExists(token, rootFolderId)) {
            Log.w("DriveVault", "jTools root folder $rootFolderId was deleted on Drive. Clearing stale links.")
            dataManager.setJtoolsFolderId(userId, "")
            dataManager.setVaultsFolderId(userId, "")
            rootFolderId = ""
        }

        if (rootFolderId.isEmpty()) {
            val (createdRootId, rootError) = createRemoteDriveFolder(token, ".jtools")
            if (createdRootId == null) {
                Log.e("DriveVault", "createVaultDriveFolder: Failed to create .jtools root folder: $rootError")
                return Pair(null, rootError)
            }
            rootFolderId = createdRootId
            dataManager.setJtoolsFolderId(userId, rootFolderId)
            createReadmeInFolder(token, rootFolderId)
        }

        var vaultsFolderId = dataManager.getVaultsFolderId()
        if (vaultsFolderId.isNotEmpty() && !verifyDriveFolderExists(token, vaultsFolderId)) {
            dataManager.setVaultsFolderId(userId, "")
            vaultsFolderId = ""
        }

        if (vaultsFolderId.isEmpty()) {
            val (createdVaultsId, vaultsError) = createRemoteDriveFolder(token, "vaults", rootFolderId)
            if (createdVaultsId == null) {
                Log.e("DriveVault", "createVaultDriveFolder: Failed to create vaults subfolder: $vaultsError")
                return Pair(null, vaultsError)
            }
            vaultsFolderId = createdVaultsId
            dataManager.setVaultsFolderId(userId, vaultsFolderId)
        }

        val (vaultFolderId, vaultError) = createRemoteDriveFolder(token, vaultName, vaultsFolderId)
        if (vaultFolderId == null) {
            Log.e("DriveVault", "createVaultDriveFolder: Failed to create $vaultName folder: $vaultError")
            return Pair(null, vaultError)
        }

        groupMemberEmails.forEach { email ->
            grantDrivePermission(token, vaultFolderId, email)
        }

        val folderUrl = buildDriveFolderUrl(vaultFolderId)
        Log.d("DriveVault", "createVaultDriveFolder: ALL OK -> ID: $vaultFolderId, URL: $folderUrl")
        return Pair(Pair(vaultFolderId, folderUrl), null)
    }

    suspend fun createSubFolder(context: Context, parentFolderId: String, folderName: String, emails: List<String>): Pair<String?, String?> = withContext(Dispatchers.IO) {
        val (token, errorMsg) = getAccessToken(context)
        if (token.isNullOrEmpty()) return@withContext Pair(null, errorMsg)
        
        val (fId, fUrl) = createRemoteDriveFolder(token, folderName, parentFolderId)
        if (fId != null) {
            emails.forEach { email -> grantDrivePermission(token, fId, email) }
        }
        return@withContext Pair(fId, fUrl)
    }

    suspend fun uploadDocumentToDrive(
        context: Context,
        parentFolderId: String,
        sourceUri: Uri,
        fileName: String,
        fileType: String,
        groupMemberEmails: List<String> = emptyList(),
        vaultId: String,
        onProgress: ((Long) -> Unit)? = null
    ): Pair<Pair<String, String>?, String?> = withContext(Dispatchers.IO) {
        if (fileName.isBlank() || vaultId.isBlank()) return@withContext Pair(null, "File name or vault ID is blank")

        val targetDir = File(context.filesDir, "vaults/$vaultId")
        if (!targetDir.exists()) targetDir.mkdirs()
        val targetFile = File(targetDir, fileName)

        val copySuccess = VaultDocUtils.copyUriToLocalFile(context, sourceUri, targetFile)
        if (!copySuccess) {
            Log.e("DriveVault", "Error saving file locally before Drive upload for $fileName")
            return@withContext Pair(null, "Local file copy error: Could not read source file")
        }

        val (token, tokenError) = getAccessToken(context)
        if (token == null) {
            return@withContext Pair(null, tokenError ?: "Not signed in with Google")
        }

        val uploadResult = uploadRemoteDriveFile(token, parentFolderId, fileName, targetFile, onProgress)
        if (uploadResult.first != null) {
            val fileId = uploadResult.first!!.first
            groupMemberEmails.forEach { email ->
                grantDrivePermission(token, fileId, email)
            }
        }
        return@withContext uploadResult
    }

    suspend fun downloadDocumentToLocal(
        context: Context,
        vaultId: String,
        doc: VaultDocument,
        index: Int = 0,
        onProgress: ((bytesDownloaded: Long, percent: Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val driveFileId = doc.driveFileIds.getOrNull(index) ?: doc.driveFileId
        if (driveFileId.isEmpty()) return@withContext ""

        val file = VaultDocUtils.getOfflineLocalFile(context, doc, index)
        if (file.exists() && file.length() > 0) {
            onProgress?.invoke(file.length(), 100)
            return@withContext file.absolutePath
        }

        try {
            val (token, tokenError) = getAccessToken(context)
            if (token.isNullOrEmpty()) {
                Log.e("DriveVault", "downloadDocumentToLocal: No token ($tokenError)")
                return@withContext ""
            }

            Log.d("DriveVault", "downloadDocumentToLocal: Downloading $driveFileId...")
            val url = URL("https://www.googleapis.com/drive/v3/files/$driveFileId?alt=media")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                if (file.parentFile != null && !file.parentFile!!.exists()) file.parentFile!!.mkdirs()
                val contentLength = conn.contentLengthLong.coerceAtLeast(1L)
                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var bytesCopied = 0L
                        var lastReportTime = 0L
                        while (true) {
                            val bytes = input.read(buffer)
                            if (bytes < 0) break
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            
                            if (onProgress != null) {
                                val now = System.currentTimeMillis()
                                if (now - lastReportTime > 400) {
                                    val percent = ((bytesCopied.toDouble() / contentLength) * 100).toInt().coerceIn(0, 99)
                                    onProgress(bytesCopied, percent)
                                    lastReportTime = now
                                }
                            }
                        }
                    }
                }
                onProgress?.invoke(file.length(), 100)
                Log.d("DriveVault", "downloadDocumentToLocal SUCCESS -> ${file.absolutePath} (${file.length()} bytes)")
                return@withContext file.absolutePath
            } else {
                Log.e("DriveVault", "downloadDocumentToLocal failed with response code $responseCode")
                return@withContext ""
            }
        } catch (e: Exception) {
            Log.e("DriveVault", "downloadDocumentToLocal exception for $driveFileId", e)
            return@withContext ""
        }
    }

    suspend fun saveRemoteDriveFileToPublicDownloads(context: Context, doc: VaultDocument, index: Int = 0): Boolean = withContext(Dispatchers.IO) {
        val driveFileId = doc.driveFileIds.getOrNull(index) ?: doc.driveFileId
        if (driveFileId.isEmpty()) return@withContext false

        val rawName = doc.fileNames.getOrNull(index) ?: doc.name
        val type = doc.fileTypes.getOrNull(index) ?: doc.fileType
        val fileName = VaultDocUtils.ensureExtension(rawName, type)

        val tempFile = File(context.cacheDir, fileName)
        try {
            val (token, tokenError) = getAccessToken(context)
            if (token.isNullOrEmpty()) return@withContext false

            val url = URL("https://www.googleapis.com/drive/v3/files/$driveFileId?alt=media")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val result = saveToPublicDownloads(context, tempFile.absolutePath, fileName, type)
                if (tempFile.exists()) tempFile.delete()
                return@withContext result
            }
            false
        } catch (e: Exception) {
            Log.e("DriveVault", "Failed to save remote file to downloads", e)
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }

    private fun resolveMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return "application/octet-stream"
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    suspend fun saveToPublicDownloads(
        context: Context,
        localPath: String,
        rawFileName: String,
        fileType: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        if (localPath.isEmpty()) return@withContext false
        val sourceFile = File(localPath)
        if (!sourceFile.exists()) return@withContext false

        return@withContext try {
            val targetFileName = VaultDocUtils.ensureExtension(rawFileName, fileType)
            val mimeType = resolveMimeType(targetFileName)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    true
                } else false
            } else {
                val targetDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, targetFileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                true
            }
        } catch (e: Exception) {
            Log.e("DriveVault", "Failed to save file to public Downloads", e)
            false
        }
    }

    suspend fun syncGroupFolderPermissions(
        context: Context,
        vaultDriveFolderId: String,
        groupMemberEmails: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val (token, _) = getAccessToken(context)
        if (token.isNullOrEmpty() || vaultDriveFolderId.isEmpty()) return@withContext false
        
        try {
            // First fetch existing permissions
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
                
                // Diff check: if the shared emails match the group emails exactly (plus creator), we can skip.
                // It's a simplistic check.
                val needsUpdate = groupMemberEmails.any { !currentSharedEmails.contains(it) } || 
                                  currentSharedEmails.any { !groupMemberEmails.contains(it) && !it.contains("google.com") }
                
                if (needsUpdate) {
                    Log.d("DriveVault", "Permissions mismatch for $vaultDriveFolderId. Updating...")
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
        return@withContext false
    }
}
