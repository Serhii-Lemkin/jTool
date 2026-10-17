package com.jtool.android.core.models.vaults

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
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
        get() = driveFileIds.firstOrNull() ?: driveFileId

    val effectiveDriveUrl: String
        get() = if (folderUrl.isNotEmpty() && (fileNames.size > 1 || driveFileIds.size > 1)) folderUrl
                else if (driveUrls.firstOrNull()?.isNotEmpty() == true) driveUrls.first()
                else if (driveUrl.isNotEmpty()) driveUrl
                else folderUrl

    val effectiveLocalPath: String
        get() = localPaths.firstOrNull { it.isNotEmpty() } ?: localPath
}
