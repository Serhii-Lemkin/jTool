package com.jtool.android.features.vaults.common

import android.content.Context
import android.util.Log
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.AttachedFile
import com.jtool.android.features.vaults.VaultsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VaultsOperationsHelper {

    fun isDocOrVaultOwner(
        doc: VaultDocument,
        vault: Vault?,
        currentUserId: String,
        authUid: String
    ): Boolean {
        val docOwnerMatch = doc.lastUpdatedBy == currentUserId ||
            (authUid.isNotEmpty() && doc.lastUpdatedBy == authUid)
        val vaultOwnerMatch = vault != null && (
            vault.createdBy == currentUserId ||
                (authUid.isNotEmpty() && vault.createdBy == authUid)
            )
        return docOwnerMatch || vaultOwnerMatch
    }

    fun mapTagToStandardKey(rawTag: String): String {
        val trimmed = rawTag.trim()
        val standardTagsMap = mapOf(
            VaultsLoc.tagWarranty to "#WARRANTY#",
            VaultsLoc.tagDocument to "#DOCUMENT#",
            VaultsLoc.tagReceipt to "#RECEIPT#",
            VaultsLoc.tagInvoice to "#INVOICE#",
            VaultsLoc.tagAgreement to "#AGREEMENT#",
            VaultsLoc.tagId to "#IDENTIFICATION#",
            VaultsLoc.tagSlip to "#SLIP#",
            VaultsLoc.tagLicense to "#LICENSE#",
            VaultsLoc.tagContract to "#CONTRACT#",
            VaultsLoc.tagInsurance to "#INSURANCE#",
            VaultsLoc.tagMedical to "#MEDICAL#",
            VaultsLoc.tagTax to "#TAX#",
            VaultsLoc.tagVehicle to "#VEHICLE#",
            VaultsLoc.tagProperty to "#PROPERTY#",
            VaultsLoc.tagFinancial to "#FINANCIAL#",
            VaultsLoc.tagCertificate to "#CERTIFICATE#",
            VaultsLoc.tagEducation to "#EDUCATION#"
        )
        return standardTagsMap[trimmed] ?: trimmed
    }

    suspend fun performUploadDoc(
        context: Context,
        dataManager: DataManager,
        gid: String,
        currentUserId: String,
        event: VaultsEvent.UploadDoc
    ): Pair<Boolean, String?> {
        val memberEmails = dataManager.getGroupMemberEmails(gid)
        val uploadedFileIds = mutableListOf<String>()
        val uploadedDriveUrls = mutableListOf<String>()
        val fileNames = mutableListOf<String>()
        val fileTypes = mutableListOf<String>()
        val storedTag = mapTagToStandardKey(event.tag)

        var docFolderId = ""
        var docFolderUrl = ""
        val targetFolderId = if (event.files.size > 1) {
            val (fId, fUrl) = GoogleDriveVaultHelper.createSubFolder(context, event.vault.driveFolderId, event.docName, memberEmails)
            docFolderId = fId ?: ""
            docFolderUrl = fUrl ?: ""
            fId ?: event.vault.driveFolderId
        } else {
            event.vault.driveFolderId
        }

        for (attachedFile in event.files) {
            val fullFileName = "${attachedFile.fileName}${attachedFile.extension}"
            val (uploadResult, errorMsg) = GoogleDriveVaultHelper.uploadDocumentToDrive(
                context, targetFolderId, attachedFile.uri, fullFileName, "document", memberEmails, event.vault.id
            )
            if (uploadResult == null) {
                return Pair(false, errorMsg ?: "Failed to upload document to Google Drive.")
            }
            uploadedFileIds.add(uploadResult.first)
            uploadedDriveUrls.add(uploadResult.second)
            fileNames.add(fullFileName)
            fileTypes.add(attachedFile.extension.removePrefix("."))
        }

        val newDoc = VaultDocument(
            vaultId = event.vault.id,
            name = event.docName,
            tag = storedTag,
            uploadTimestamp = System.currentTimeMillis(),
            documentTimestamp = event.docDate,
            description = event.description,
            driveFileId = uploadedFileIds.firstOrNull() ?: "",
            driveUrl = uploadedDriveUrls.firstOrNull() ?: "",
            fileType = fileTypes.firstOrNull() ?: "",
            driveFileIds = uploadedFileIds,
            driveUrls = uploadedDriveUrls,
            fileNames = fileNames,
            fileTypes = fileTypes,
            folderId = docFolderId,
            folderUrl = docFolderUrl,
            lastUpdatedBy = currentUserId
        )

        val docId = dataManager.addVaultDoc(gid, event.vault.id, newDoc)
        if (event.saveOffline) {
            try {
                val docDir = File(context.filesDir, "vaults/${event.vault.id}/$docId")
                if (!docDir.exists()) docDir.mkdirs()
                fileNames.forEach { fileName ->
                    val srcFile = File(context.filesDir, "vaults/${event.vault.id}/$fileName")
                    if (srcFile.exists()) {
                        srcFile.copyTo(File(docDir, fileName), overwrite = true)
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultsOperationsHelper", "Failed to cache files locally", e)
            }
        }

        fileNames.forEach { fileName ->
            val srcFile = File(context.filesDir, "vaults/${event.vault.id}/$fileName")
            if (srcFile.exists()) srcFile.delete()
        }

        return Pair(true, null)
    }

    suspend fun performUpdateDoc(
        context: Context,
        dataManager: DataManager,
        gid: String,
        currentUserId: String,
        authUid: String,
        vault: Vault,
        eventDoc: VaultDocument,
        oldDoc: VaultDocument?,
        newFilesToUpload: List<AttachedFile>,
        removedDriveFileIds: List<String>
    ): Pair<VaultDocument?, String?> {
        val baseDoc = oldDoc ?: eventDoc
        val isOwner = isDocOrVaultOwner(baseDoc, vault, currentUserId, authUid)
        val storedTag = mapTagToStandardKey(eventDoc.tag)

        if (!isOwner) {
            val updatedDoc = baseDoc.copy(
                name = eventDoc.name,
                tag = storedTag,
                documentTimestamp = eventDoc.documentTimestamp,
                description = eventDoc.description
            )
            dataManager.updateVaultDoc(gid, eventDoc.vaultId, updatedDoc)
            return Pair(updatedDoc, null)
        }

        val memberEmails = dataManager.getGroupMemberEmails(gid)

        if (oldDoc != null && oldDoc.name != eventDoc.name && eventDoc.folderId.isNotEmpty()) {
            try {
                GoogleDriveVaultHelper.renameRemoteDriveItem(context, eventDoc.folderId, eventDoc.name)
            } catch (e: Exception) {
                Log.e("VaultsOperationsHelper", "Failed to rename folder on Drive", e)
            }
        }

        removedDriveFileIds.filter { it.isNotEmpty() }.forEach { id ->
            val index = oldDoc?.driveFileIds?.indexOf(id) ?: -1
            if (index != -1 && oldDoc != null) {
                VaultDocUtils.deleteLocalOfflineFile(context, oldDoc, index)
            }
            GoogleDriveVaultHelper.deleteRemoteDriveItem(context, id)
        }

        val newDriveFileIds = mutableListOf<String>()
        val newDriveUrls = mutableListOf<String>()
        val newFileNames = mutableListOf<String>()
        val newFileTypes = mutableListOf<String>()

        var docFolderId = eventDoc.folderId
        var docFolderUrl = eventDoc.folderUrl

        if (docFolderId.isEmpty() && newFilesToUpload.isNotEmpty()) {
            val (fId, fUrl) = GoogleDriveVaultHelper.createSubFolder(context, vault.driveFolderId, eventDoc.name, memberEmails)
            docFolderId = fId ?: ""
            docFolderUrl = fUrl ?: ""
        }

        val targetFolderId = if (docFolderId.isNotEmpty()) docFolderId else vault.driveFolderId

        for (attachedFile in newFilesToUpload) {
            val fullFileName = "${attachedFile.fileName}${attachedFile.extension}"
            val (uploadResult, errorMsg) = GoogleDriveVaultHelper.uploadDocumentToDrive(
                context, targetFolderId, attachedFile.uri, fullFileName, "document", memberEmails, vault.id
            )
            if (uploadResult != null) {
                newDriveFileIds.add(uploadResult.first)
                newDriveUrls.add(uploadResult.second)
                newFileNames.add(fullFileName)
                newFileTypes.add(attachedFile.extension.removePrefix("."))
            } else {
                return Pair(null, errorMsg ?: "Failed to upload file to Google Drive.")
            }
        }

        val finalDriveFileIds = eventDoc.driveFileIds + newDriveFileIds
        val finalDriveUrls = eventDoc.driveUrls + newDriveUrls
        val finalFileNames = eventDoc.fileNames + newFileNames
        val finalFileTypes = eventDoc.fileTypes + newFileTypes

        val updatedDoc = eventDoc.copy(
            tag = storedTag,
            driveFileId = finalDriveFileIds.firstOrNull() ?: eventDoc.driveFileId,
            driveUrl = finalDriveUrls.firstOrNull() ?: eventDoc.driveUrl,
            fileType = finalFileTypes.firstOrNull() ?: eventDoc.fileType,
            driveFileIds = finalDriveFileIds,
            driveUrls = finalDriveUrls,
            fileNames = finalFileNames,
            fileTypes = finalFileTypes,
            folderId = docFolderId,
            folderUrl = docFolderUrl,
            lastUpdatedBy = currentUserId
        )

        dataManager.updateVaultDoc(gid, eventDoc.vaultId, updatedDoc)
        return Pair(updatedDoc, null)
    }

    suspend fun performDeleteDoc(
        context: Context,
        dataManager: DataManager,
        gid: String,
        vaultId: String,
        docId: String,
        doc: VaultDocument?
    ) {
        if (doc != null) {
            if (doc.folderId.isNotEmpty()) {
                GoogleDriveVaultHelper.deleteRemoteDriveItem(context, doc.folderId)
            }
            val driveIds = (doc.driveFileIds + doc.driveFileId + doc.effectiveDriveFileId)
                .filter { it.isNotEmpty() && it != doc.folderId }
                .distinct()
            driveIds.forEach { id ->
                GoogleDriveVaultHelper.deleteRemoteDriveItem(context, id)
            }
            VaultDocUtils.deleteAllOfflineFiles(context, doc)
        }
        dataManager.deleteVaultDoc(gid, vaultId, docId)
    }

    suspend fun performDeleteSpecificFile(
        context: Context,
        dataManager: DataManager,
        gid: String,
        doc: VaultDocument,
        fileIndex: Int,
        currentUserId: String
    ): VaultDocument {
        val fileId = doc.driveFileIds.getOrNull(fileIndex)
        if (!fileId.isNullOrEmpty()) {
            GoogleDriveVaultHelper.deleteRemoteDriveItem(context, fileId)
        }

        VaultDocUtils.deleteLocalOfflineFile(context, doc, fileIndex)

        val newFileIds = doc.driveFileIds.toMutableList().apply { if (size > fileIndex) removeAt(fileIndex) }
        val newUrls = doc.driveUrls.toMutableList().apply { if (size > fileIndex) removeAt(fileIndex) }
        val newNames = doc.fileNames.toMutableList().apply { if (size > fileIndex) removeAt(fileIndex) }
        val newTypes = doc.fileTypes.toMutableList().apply { if (size > fileIndex) removeAt(fileIndex) }

        val updatedDoc = doc.copy(
            driveFileIds = newFileIds,
            driveUrls = newUrls,
            fileNames = newNames,
            fileTypes = newTypes,
            driveFileId = newFileIds.firstOrNull() ?: "",
            driveUrl = newUrls.firstOrNull() ?: "",
            fileType = newTypes.firstOrNull() ?: "",
            lastUpdatedBy = currentUserId
        )

        dataManager.updateVaultDoc(gid, doc.vaultId, updatedDoc)
        return updatedDoc
    }

    suspend fun validateCachedData(
        context: Context,
        dataManager: DataManager,
        gid: String,
        specificVaultId: String? = null
    ): Int = withContext(Dispatchers.IO) {
        var removedCount = 0
        try {
            val vaultsDir = File(context.filesDir, "vaults")
            if (!vaultsDir.exists()) return@withContext 0

            val serverVaults = dataManager.getVaultsDirect(gid)
            val validVaultIds = serverVaults.map { it.id }.toSet()

            val localVaultFolders = vaultsDir.listFiles() ?: emptyArray()
            for (vFolder in localVaultFolders) {
                if (!vFolder.isDirectory) continue
                val vId = vFolder.name

                if (!validVaultIds.contains(vId)) {
                    vFolder.deleteRecursively()
                    removedCount++
                    continue
                }

                if (specificVaultId != null && vId != specificVaultId) continue

                val serverDocs = dataManager.getVaultDocsDirect(gid, vId)
                val validDocMap = serverDocs.associateBy { it.id }

                val docEntries = vFolder.listFiles() ?: emptyArray()
                for (docEntry in docEntries) {
                    if (docEntry.isDirectory) {
                        val docId = docEntry.name
                        val serverDoc = validDocMap[docId]
                        if (serverDoc == null) {
                            docEntry.deleteRecursively()
                            removedCount++
                        } else {
                            val expectedFileNames = buildSet {
                                addAll(serverDoc.fileNames)
                                serverDoc.fileNames.indices.forEach { idx ->
                                    add(
                                        VaultDocUtils.ensureExtension(
                                            serverDoc.fileNames[idx],
                                            serverDoc.fileTypes.getOrElse(idx) { serverDoc.fileType }
                                        )
                                    )
                                }
                            }
                            val docFiles = docEntry.listFiles() ?: emptyArray()
                            for (dFile in docFiles) {
                                if (expectedFileNames.isNotEmpty() && !expectedFileNames.contains(dFile.name)) {
                                    dFile.delete()
                                    removedCount++
                                }
                            }
                        }
                    } else {
                        docEntry.delete()
                        removedCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VaultsOperationsHelper", "Failed to validate cached data", e)
        }
        removedCount
    }
}
