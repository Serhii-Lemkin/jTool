package com.jtool.android.features.vaults

import android.content.Context
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.persistence.LocalPersistenceManager
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum
import java.io.File

class VaultsManager(context: Context) : LocalPersistenceManager<VaultDocument>(context, PersistencePrefixEnum.LINKS) {

    override fun save(items: List<VaultDocument>, groupId: String?) {
        val encoded = items.joinToString(";") {
            "${it.id}|${it.vaultId}|${it.name}|${it.tag}|${it.uploadTimestamp}|${it.documentTimestamp}|${it.description}|${it.driveFileId}|${it.driveUrl}|${it.localPath}|${it.fileType}|${it.fileSize}|${it.lastUpdatedBy}"
        }
        getPrefs(groupId).edit().putString("vault_docs", encoded).apply()
    }

    override fun load(groupId: String?): List<VaultDocument> {
        val encoded = getPrefs(groupId).getString("vault_docs", "") ?: ""
        if (encoded.isEmpty()) return emptyList()

        return encoded.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 10) {
                VaultDocument(
                    id = parts[0],
                    vaultId = parts[1],
                    name = parts[2],
                    tag = parts[3],
                    uploadTimestamp = parts[4].toLongOrNull() ?: 0L,
                    documentTimestamp = parts[5].toLongOrNull() ?: 0L,
                    description = parts[6],
                    driveFileId = parts[7],
                    driveUrl = parts[8],
                    localPath = parts[9],
                    fileType = if (parts.size >= 11) parts[10] else "",
                    fileSize = if (parts.size >= 12) parts[11].toLongOrNull() ?: 0L else 0L,
                    lastUpdatedBy = if (parts.size >= 13) parts[12] else ""
                )
            } else null
        }
    }

    fun isFileDownloaded(localPath: String): Boolean {
        if (localPath.isEmpty()) return false
        return File(localPath).exists()
    }
}
