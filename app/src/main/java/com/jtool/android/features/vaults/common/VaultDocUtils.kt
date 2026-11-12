package com.jtool.android.features.vaults.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.localization.domains.VaultsLoc
import java.io.File

object VaultDocUtils {

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        result = c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            } catch (_: Exception) {}
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "file"
    }

    fun getExtFromUri(context: Context, uri: Uri): String {
        val fileName = getFileNameFromUri(context, uri)
        return if (fileName.contains('.')) {
            fileName.substringAfterLast('.').lowercase()
        } else {
            val mimeType = context.contentResolver.getType(uri)
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "file"
        }
    }

    fun getLocalizedTag(tagKey: String): String {
        val clean = tagKey.trim().removeSurrounding("#").uppercase()
        return when (clean) {
            "WARRANTY", "גאראנטיה", "אחריות" -> VaultsLoc.tagWarranty
            "DOCUMENT", "מסמך", "ДОКУМЕНТ" -> VaultsLoc.tagDocument
            "RECEIPT", "קבלה", "ЧЕК" -> VaultsLoc.tagReceipt
            "INVOICE", "חשבונית", "РАХУНОК" -> VaultsLoc.tagInvoice
            "AGREEMENT", "הסכם", "ДОГОВІР" -> VaultsLoc.tagAgreement
            "IDENTIFICATION", "ID", "תעודה מזהה", "ПОСВІДЧЕННЯ" -> VaultsLoc.tagId
            "SLIP", "תלוש", "КВИТАНЦІЯ" -> VaultsLoc.tagSlip
            "LICENSE", "רישיון", "ЛІЦЕНЗІЯ / ПРАВА", "ЛІЦЕНЗІЯ" -> VaultsLoc.tagLicense
            "CONTRACT", "חוזה" -> VaultsLoc.tagContract
            "INSURANCE", "ביטוח", "СТРАХОВКА" -> VaultsLoc.tagInsurance
            "MEDICAL", "רפואי", "МЕДИЧНИЙ" -> VaultsLoc.tagMedical
            "TAX", "מיסים", "ПОДАТКИ" -> VaultsLoc.tagTax
            "VEHICLE", "רכב", "ТРАНСПОРТ" -> VaultsLoc.tagVehicle
            "PROPERTY", "נכס", "НЕРУХОМІСТЬ" -> VaultsLoc.tagProperty
            "FINANCIAL", "פיננסי", "ФІНАНСИ" -> VaultsLoc.tagFinancial
            "CERTIFICATE", "תעודה", "СЕРТИФІКАТ" -> VaultsLoc.tagCertificate
            "EDUCATION", "חינוך", "ОСВІТА" -> VaultsLoc.tagEducation
            else -> {
                if (tagKey.startsWith("#") && tagKey.endsWith("#")) {
                    tagKey.removeSurrounding("#")
                } else {
                    tagKey
                }
            }
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (cm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val network = cm.activeNetwork ?: return false
                    val capabilities = cm.getNetworkCapabilities(network) ?: return false
                    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } else {
                    @Suppress("DEPRECATION")
                    val info = cm.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    return info != null && info.isConnected
                }
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun isWifiConnected(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (cm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val network = cm.activeNetwork ?: return false
                    val capabilities = cm.getNetworkCapabilities(network) ?: return false
                    return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                } else {
                    @Suppress("DEPRECATION")
                    val info = cm.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    return info != null && info.isConnected && info.type == android.net.ConnectivityManager.TYPE_WIFI
                }
            }
        } catch (e: Exception) {
            Log.e("VaultDocUtils", "Error checking Wi-Fi", e)
        }
        return false
    }

    fun copyUriToLocalFile(context: Context, sourceUri: Uri, targetFile: File): Boolean {
        try {
            if (targetFile.parentFile != null && !targetFile.parentFile!!.exists()) {
                targetFile.parentFile!!.mkdirs()
            }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                java.io.FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) return true
        } catch (e: Exception) {
            Log.w("VaultDocUtils", "openInputStream failed for $sourceUri", e)
        }

        try {
            val path = sourceUri.path
            if (!path.isNullOrEmpty()) {
                val srcFile = File(path)
                if (srcFile.exists() && srcFile.length() > 0) {
                    srcFile.copyTo(targetFile, overwrite = true)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w("VaultDocUtils", "Direct file copy failed for $sourceUri", e)
        }

        try {
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { input ->
                    java.io.FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) return true
        } catch (e: Exception) {
            Log.e("VaultDocUtils", "openFileDescriptor copy failed for $sourceUri", e)
        }

        return targetFile.exists() && targetFile.length() > 0
    }

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        val kb = sizeBytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(java.util.Locale.getDefault(), "%.2f GB", gb)
    }

    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (index != -1) {
                            val size = cursor.getLong(index)
                            if (size > 0) return size
                        }
                    }
                }
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val size = pfd.statSize
                    if (size > 0) return size
                }
            } else if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) return f.length()
            }
            0L
        } catch (e: Exception) {
            Log.e("VaultDocUtils", "Error getting file size for $uri", e)
            0L
        }
    }

    fun getFileExtensionIcon(fileName: String): ImageVector {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> Icons.Default.PictureAsPdf
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> Icons.Default.Image
            "txt", "json", "csv", "log", "md" -> Icons.Default.Article
            "doc", "docx" -> Icons.Default.Description
            "xls", "xlsx" -> Icons.Default.TableChart
            "zip", "rar", "7z", "tar", "gz" -> Icons.Default.FolderZip
            "mp3", "wav", "m4a", "aac" -> Icons.Default.AudioFile
            "mp4", "mkv", "avi", "mov" -> Icons.Default.VideoFile
            else -> Icons.Default.Description
        }
    }

    fun ensureExtension(fileName: String, fileType: String = ""): String {
        if (fileName.contains('.')) return fileName
        val cleanType = fileType.trim().removePrefix(".").lowercase()
        val ext = when {
            cleanType.isNotBlank() && cleanType != "document" && cleanType != "file" -> cleanType
            else -> "pdf"
        }
        return "$fileName.$ext"
    }

    fun getOfflineLocalFile(context: Context, doc: VaultDocument, index: Int = 0): File {
        val rawName = doc.fileNames.getOrNull(index) ?: doc.name
        val type = doc.fileTypes.getOrNull(index) ?: doc.fileType
        val fileName = ensureExtension(rawName, type)

        // 1. Check doc-specific directory with extension
        val docDirFile = File(context.filesDir, "vaults/${doc.vaultId}/${doc.id}/$fileName")
        if (docDirFile.exists() && docDirFile.length() > 0) return docDirFile

        // 2. Check localPaths or localPath if populated
        val customPath = doc.localPaths.getOrNull(index) ?: if (index == 0) doc.localPath else ""
        if (customPath.isNotEmpty()) {
            val customFile = File(customPath)
            if (customFile.exists() && customFile.length() > 0) return customFile
        }

        // 3. Check doc-specific directory with rawName
        if (rawName != fileName) {
            val rawDocDirFile = File(context.filesDir, "vaults/${doc.vaultId}/${doc.id}/$rawName")
            if (rawDocDirFile.exists() && rawDocDirFile.length() > 0) return rawDocDirFile
        }

        // 4. Check legacy prefixed file name
        val legacyFile = File(context.filesDir, "vaults/${doc.vaultId}/${doc.id}_${doc.name}")
        if (legacyFile.exists() && legacyFile.length() > 0) return legacyFile

        val targetDir = File(context.filesDir, "vaults/${doc.vaultId}/${doc.id}")
        if (!targetDir.exists()) targetDir.mkdirs()
        return docDirFile
    }

    fun isDownloadedLocally(context: Context, doc: VaultDocument, index: Int = 0): Boolean {
        if (doc.id.isBlank() || doc.vaultId.isBlank()) return false
        val file = getOfflineLocalFile(context, doc, index)
        return file.exists() && file.length() > 0
    }

    fun isDocumentDownloadedLocally(context: Context, doc: VaultDocument): Boolean {
        if (doc.id.isBlank() || doc.vaultId.isBlank()) return false
        val count = if (doc.fileNames.isNotEmpty()) doc.fileNames.size else if (doc.driveFileIds.isNotEmpty()) doc.driveFileIds.size else 1
        for (i in 0 until count) {
            if (!isDownloadedLocally(context, doc, i)) return false
        }
        return true
    }

    fun deleteLocalOfflineFile(context: Context, doc: VaultDocument, index: Int = 0): Boolean {
        return try {
            val file = getOfflineLocalFile(context, doc, index)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            Log.e("VaultDocUtils", "Failed to delete offline file for ${doc.name}", e)
            false
        }
    }

    fun deleteAllOfflineFiles(context: Context, doc: VaultDocument): Boolean {
        return try {
            val docDir = File(context.filesDir, "vaults/${doc.vaultId}/${doc.id}")
            if (docDir.exists()) {
                docDir.deleteRecursively()
            }
            doc.fileNames.forEach { fileName ->
                val vFile = File(context.filesDir, "vaults/${doc.vaultId}/$fileName")
                if (vFile.exists()) vFile.delete()
            }
            val nameFile = File(context.filesDir, "vaults/${doc.vaultId}/${doc.name}")
            if (nameFile.exists()) nameFile.delete()

            if (doc.fileType.isNotBlank()) {
                val typedFile = File(context.filesDir, "vaults/${doc.vaultId}/${doc.name}.${doc.fileType.removePrefix(".")}")
                if (typedFile.exists()) typedFile.delete()
            }
            val legacyFile = File(context.filesDir, "vaults/${doc.vaultId}/${doc.id}_${doc.name}")
            if (legacyFile.exists()) {
                legacyFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e("VaultDocUtils", "Failed to delete offline files for ${doc.name}", e)
            false
        }
    }

    fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> "image/*"
            "mp4", "mkv", "avi", "mov", "3gp", "webm" -> "video/*"
            "mp3", "wav", "m4a", "aac", "flac", "ogg" -> "audio/*"
            "txt", "json", "csv", "log", "md" -> "text/plain"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "zip", "rar", "7z", "tar", "gz" -> "application/zip"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        }
    }

    fun openLocalFile(context: Context, doc: VaultDocument, index: Int = 0): Boolean {
        return try {
            val file = getOfflineLocalFile(context, doc, index)
            if (!file.exists() || file.length() == 0L) return false

            var fileName = doc.fileNames.getOrNull(index) ?: doc.name
            val type = doc.fileTypes.getOrNull(index) ?: doc.fileType
            if (!fileName.contains('.') && type.isNotBlank()) {
                fileName = "$fileName.${type.removePrefix(".")}"
            }

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val mimeType = getMimeType(fileName)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                context.startActivity(Intent.createChooser(intent, fileName))
            }
            true
        } catch (e: Exception) {
            Log.e("VaultDocUtils", "Failed to open local file: ${doc.name}", e)
            false
        }
    }
}

@Composable
fun DocIconWithBadge(
    fileName: String,
    isDownloaded: Boolean,
    isDownloading: Boolean = false,
    isDeleting: Boolean = false,
    isSavingToDownloads: Boolean = false,
    modifier: Modifier = Modifier
) {
    val mainIcon = remember(fileName) { VaultDocUtils.getFileExtensionIcon(fileName) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = mainIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        if (isDeleting) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.error
            )
        } else if (isDownloading || isSavingToDownloads) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (isDownloaded) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.OfflinePin,
                    contentDescription = "Downloaded",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
