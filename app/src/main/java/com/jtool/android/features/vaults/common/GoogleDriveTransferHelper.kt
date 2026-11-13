package com.jtool.android.features.vaults.common

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.jtool.android.core.models.vaults.VaultDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object GoogleDriveTransferHelper {

    fun extractDriveFileIdFromUrl(url: String): String {
        if (url.isBlank()) return ""
        val dMatch = Regex("/d/([a-zA-Z0-9_-]+)").find(url)
        if (dMatch != null) return dMatch.groupValues[1]
        val idMatch = Regex("[?&]id=([a-zA-Z0-9_-]+)").find(url)
        if (idMatch != null) return idMatch.groupValues[1]
        return ""
    }

    fun resolveDriveFileId(doc: VaultDocument, index: Int): String {
        val byIndex = doc.driveFileIds.getOrNull(index)?.trim().orEmpty()
        if (byIndex.isNotEmpty() && !byIndex.startsWith("jtools_") && !byIndex.startsWith("local_")) {
            return byIndex
        }
        if (index == 0 && doc.driveFileId.isNotBlank() && !doc.driveFileId.startsWith("jtools_") && !doc.driveFileId.startsWith("local_")) {
            return doc.driveFileId.trim()
        }
        val urlByIndex = doc.driveUrls.getOrNull(index).orEmpty()
        val fromIndexUrl = extractDriveFileIdFromUrl(urlByIndex)
        if (fromIndexUrl.isNotEmpty()) return fromIndexUrl
        if (index == 0) {
            val fromMainUrl = extractDriveFileIdFromUrl(doc.driveUrl)
            if (fromMainUrl.isNotEmpty()) return fromMainUrl
        }
        return ""
    }

    suspend fun uploadRemoteDriveFile(
        token: String,
        parentFolderId: String,
        fileName: String,
        sourceFile: File,
        onProgress: ((Long) -> Unit)? = null
    ): Pair<Pair<String, String>?, String?> = withContext(Dispatchers.IO) {
        try {
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
                    put("parents", JSONArray().put(parentFolderId))
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
            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val fileId = json.optString("id")
                if (fileId.isNotEmpty()) {
                    Pair(Pair(fileId, GoogleDriveVaultHelper.buildDriveFileUrl(fileId)), null)
                } else {
                    Pair(null, "Empty file ID returned")
                }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val cleanMsg = GoogleDriveVaultHelper.parseApiErrorMessage(errorBody)
                Log.e("DriveVault", "uploadRemoteDriveFile: API Error ($responseCode): $cleanMsg")
                Pair(null, cleanMsg)
            }
        } catch (e: Exception) {
            Log.e("DriveVault", "uploadRemoteDriveFile: Exception for '$fileName'", e)
            Pair(null, "Upload Exception: ${e.message}")
        }
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
            return@withContext Pair(null, "Local file copy error: Could not read source file")
        }

        val (token, tokenError) = GoogleDriveVaultHelper.fetchAccessToken(context)
        if (token == null) {
            return@withContext Pair(null, tokenError ?: "Not signed in with Google")
        }

        val uploadResult = uploadRemoteDriveFile(token, parentFolderId, fileName, targetFile, onProgress)
        if (uploadResult.first != null) {
            val fileId = uploadResult.first!!.first
            groupMemberEmails.forEach { email ->
                GoogleDriveVaultHelper.grantDrivePermissionWithToken(token, fileId, email)
            }
        }
        return@withContext uploadResult
    }

    private suspend fun openAuthorizedDownloadConnection(
        context: Context,
        driveFileId: String
    ): HttpURLConnection? = withContext(Dispatchers.IO) {
        val (token, tokenError) = GoogleDriveVaultHelper.fetchAccessToken(context)
        if (token.isNullOrEmpty()) {
            Log.e("DriveVault", "openAuthorizedDownloadConnection: No token ($tokenError)")
            return@withContext null
        }

        val apiUrl = "https://www.googleapis.com/drive/v3/files/$driveFileId?alt=media"
        var conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 20_000
            readTimeout = 30_000
        }

        if (conn.responseCode == 401) {
            conn.disconnect()
            GoogleDriveVaultHelper.clearCachedToken(context, token)
            val (freshToken, _) = GoogleDriveVaultHelper.fetchAccessToken(context)
            if (!freshToken.isNullOrEmpty()) {
                conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $freshToken")
                    connectTimeout = 20_000
                    readTimeout = 30_000
                }
            }
        }
        conn
    }

    private fun tryFallbackPublicStream(driveFileId: String): HttpURLConnection? {
        return try {
            val fallbackUrl = URL("https://drive.google.com/uc?export=download&id=$driveFileId")
            val conn = (fallbackUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 20_000
                readTimeout = 30_000
            }
            val code = conn.responseCode
            val contentType = conn.contentType?.lowercase().orEmpty()
            if (code in 200..299 && !contentType.startsWith("text/html")) {
                conn
            } else {
                conn.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun copyStreamToFile(
        conn: HttpURLConnection,
        targetFile: File,
        onProgress: ((Long, Int) -> Unit)? = null
    ): Boolean {
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        val tmpFile = File(parent ?: targetFile.parentFile, "${targetFile.name}.tmp")
        return try {
            val contentLength = conn.contentLengthLong.coerceAtLeast(1L)
            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
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
            if (!tmpFile.exists() || tmpFile.length() == 0L) {
                tmpFile.delete()
                return false
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tmpFile.renameTo(targetFile)) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }
            onProgress?.invoke(targetFile.length(), 100)
            true
        } catch (e: Exception) {
            Log.e("DriveVault", "copyStreamToFile failed for ${targetFile.name}", e)
            if (tmpFile.exists()) tmpFile.delete()
            false
        }
    }

    suspend fun downloadDocumentToLocal(
        context: Context,
        vaultId: String,
        doc: VaultDocument,
        index: Int = 0,
        onProgress: ((bytesDownloaded: Long, percent: Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val driveFileId = resolveDriveFileId(doc, index)
        if (driveFileId.isEmpty()) return@withContext ""

        val file = VaultDocUtils.getOfflineLocalFile(context, doc, index)
        if (file.exists() && file.length() > 0) {
            onProgress?.invoke(file.length(), 100)
            return@withContext file.absolutePath
        }

        var conn: HttpURLConnection? = null
        try {
            conn = openAuthorizedDownloadConnection(context, driveFileId)
            if (conn != null && conn.responseCode in 200..299) {
                if (copyStreamToFile(conn, file, onProgress)) {
                    return@withContext file.absolutePath
                }
            } else if (conn != null) {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("DriveVault", "downloadDocumentToLocal failed (${conn.responseCode}): ${GoogleDriveVaultHelper.parseApiErrorMessage(errBody)}")
            }
            conn?.disconnect()

            val fallbackConn = tryFallbackPublicStream(driveFileId)
            if (fallbackConn != null) {
                try {
                    if (copyStreamToFile(fallbackConn, file, onProgress)) {
                        return@withContext file.absolutePath
                    }
                } finally {
                    fallbackConn.disconnect()
                }
            }
            return@withContext ""
        } catch (e: Exception) {
            Log.e("DriveVault", "downloadDocumentToLocal exception for $driveFileId", e)
            return@withContext ""
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun saveRemoteDriveFileToPublicDownloads(
        context: Context,
        doc: VaultDocument,
        index: Int = 0
    ): Boolean = withContext(Dispatchers.IO) {
        val driveFileId = resolveDriveFileId(doc, index)
        if (driveFileId.isEmpty()) return@withContext false

        val rawName = doc.fileNames.getOrNull(index) ?: doc.name
        val type = doc.fileTypes.getOrNull(index) ?: doc.fileType
        val fileName = VaultDocUtils.ensureExtension(rawName, type)
        val tempFile = File(context.cacheDir, fileName)

        var conn: HttpURLConnection? = null
        try {
            conn = openAuthorizedDownloadConnection(context, driveFileId)
            val downloaded = if (conn != null && conn.responseCode in 200..299) {
                copyStreamToFile(conn, tempFile)
            } else {
                conn?.disconnect()
                val fallback = tryFallbackPublicStream(driveFileId)
                if (fallback != null) {
                    try {
                        copyStreamToFile(fallback, tempFile)
                    } finally {
                        fallback.disconnect()
                    }
                } else {
                    false
                }
            }
            if (!downloaded) return@withContext false
            val result = saveToPublicDownloads(context, tempFile.absolutePath, fileName, type)
            if (tempFile.exists()) tempFile.delete()
            result
        } catch (e: Exception) {
            Log.e("DriveVault", "Failed to save remote file to downloads", e)
            if (tempFile.exists()) tempFile.delete()
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun resolveMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    true
                } else {
                    false
                }
            } else {
                val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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
}
