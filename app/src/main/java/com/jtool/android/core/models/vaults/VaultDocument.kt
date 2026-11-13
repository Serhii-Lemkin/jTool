package com.jtool.android.core.models.vaults

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Keep
@IgnoreExtraProperties
@Serializable
data class VaultDocument(
    var id: String = "",
    val vaultId: String = "",
    val name: String = "",
    val tag: String = "",
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val documentTimestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val driveFileId: String = "",
    val driveUrl: String = "",
    val localPath: String = "",
    val fileType: String = "",
    val fileSize: Long = 0L,
    val lastUpdatedBy: String = "",
    val folderId: String = "",
    val folderUrl: String = "",
    val driveFileIds: List<String> = emptyList(),
    val driveUrls: List<String> = emptyList(),
    val localPaths: List<String> = emptyList(),
    val fileNames: List<String> = emptyList(),
    val fileTypes: List<String> = emptyList()
) {
    val effectiveDriveFileId: String
        get() = driveFileIds.firstOrNull { it.isNotBlank() } ?: driveFileId

    val effectiveFolderUrl: String
        get() = folderUrl.ifBlank {
            if (folderId.isNotBlank()) "https://drive.google.com/drive/folders/$folderId" else ""
        }

    val effectiveDriveUrl: String
        get() = if (effectiveFolderUrl.isNotEmpty() && (fileNames.size > 1 || driveFileIds.size > 1)) effectiveFolderUrl
                else if (driveUrls.firstOrNull()?.isNotBlank() == true) driveUrls.first()
                else if (driveUrl.isNotBlank()) driveUrl
                else if (effectiveDriveFileId.isNotBlank()) "https://drive.google.com/file/d/$effectiveDriveFileId/view"
                else effectiveFolderUrl

    fun getFileDriveUrl(index: Int): String {
        val explicitUrl = driveUrls.getOrNull(index)
        if (!explicitUrl.isNullOrBlank()) return explicitUrl
        val fileId = driveFileIds.getOrNull(index)?.takeIf { it.isNotBlank() }
            ?: if (index == 0) driveFileId.takeIf { it.isNotBlank() } else null
        if (!fileId.isNullOrBlank()) return "https://drive.google.com/file/d/$fileId/view"
        return effectiveDriveUrl
    }

    val effectiveLocalPath: String
        get() = localPaths.firstOrNull { it.isNotEmpty() } ?: localPath
}
