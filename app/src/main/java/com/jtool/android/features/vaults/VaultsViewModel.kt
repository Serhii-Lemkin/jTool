package com.jtool.android.features.vaults

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.core.localization.domains.VaultsLoc
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class VaultsViewModel(
    private val vaultsManager: VaultsManager,
    private val settingsManager: SettingsManager,
    private val dataManager: DataManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultsUiState())
    val uiState: StateFlow<VaultsUiState> = _uiState.asStateFlow()

    private var groupId: String? = null
    private var vaultsJob: Job? = null
    private var docsJob: Job? = null

    private val _allGroupDocs = MutableStateFlow<List<VaultDocument>>(emptyList())
    val allGroupDocs: StateFlow<List<VaultDocument>> = _allGroupDocs.asStateFlow()

    private fun loadAllGroupDocs(gid: String) {
        viewModelScope.launch {
            dataManager.getVaultsFlow(gid).collect { vaultsList ->
                val docsList = mutableListOf<VaultDocument>()
                vaultsList.forEach { v ->
                    val docs = dataManager.getVaultDocsDirect(gid, v.id)
                    docsList.addAll(docs)
                }
                _allGroupDocs.value = docsList
            }
        }
    }

    init {
        dataManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val seenInfo = settingsManager.getSeenVaultsInfo()
                    if (!seenInfo) {
                        dataManager.setSeenVaultsInfo(uid, true)
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultsViewModel", "Failed silent seenVaultsInfo update", e)
            }
        }
    }

    fun markVaultsInfoSeen(uid: String) {
        viewModelScope.launch {
            dataManager.setSeenVaultsInfo(uid, true)
        }
    }

    fun setGroupId(id: String?) {
        if (groupId == id) return
        groupId = id
        observeVaults()
    }

    private fun observeVaults() {
        val gid = groupId ?: return
        loadAllGroupDocs(gid)
        vaultsJob?.cancel()
        vaultsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val directVaults = dataManager.getVaultsDirect(gid)
                _uiState.update { it.copy(vaults = directVaults) }
                validateCachedData(gid)
            } catch (e: Exception) {
                Log.e("VaultsViewModel", "Failed direct vaults fetch", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }

            dataManager.getVaultsFlow(gid)
                .onEach { list ->
                    _uiState.update { it.copy(vaults = list) }
                    viewModelScope.launch {
                        try {
                            val memberEmails = dataManager.getGroupMemberEmails(gid)
                            list.forEach { vault ->
                                GoogleDriveVaultHelper.syncGroupFolderPermissions(context, vault.driveFolderId, memberEmails)
                            }
                        } catch (e: Exception) { Log.e("VaultsViewModel", "Failed silent BG permission sync", e) }
                    }
                }
                .launchIn(this)
        }
    }

    fun selectVault(vault: Vault?) {
        _uiState.update { it.copy(selectedVault = vault, docs = emptyList(), isLoading = vault != null) }
        docsJob?.cancel()
        val gid = groupId ?: return
        if (vault != null) {
            docsJob = viewModelScope.launch {
                try {
                    val directDocs = dataManager.getVaultDocsDirect(gid, vault.id)
                    _uiState.update { it.copy(docs = directDocs) }
                    validateCachedData(gid, vault.id)
                } catch (e: Exception) {
                    Log.e("VaultsViewModel", "Failed direct docs fetch", e)
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }

                dataManager.getVaultDocsFlow(gid, vault.id)
                    .onEach { list ->
                        _uiState.update { it.copy(docs = list) }
                        vaultsManager.save(list, gid)
                    }
                    .launchIn(this)
            }
        }
    }

    fun selectDoc(doc: VaultDocument?) {
        _uiState.update { it.copy(selectedDoc = doc) }
    }

    fun onEvent(event: VaultsEvent) {
        val gid = groupId ?: return
        when (event) {
            is VaultsEvent.SetGroupId -> setGroupId(event.groupId)
            is VaultsEvent.ToggleSearch -> _uiState.update { it.copy(isSearchVisible = !it.isSearchVisible) }
            is VaultsEvent.LoadMore -> _uiState.update { it.copy(pagingLimit = it.pagingLimit + 15) }
            is VaultsEvent.CreateVault -> {
                viewModelScope.launch {
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUid != null) {
                        try {
                            val memberEmails = dataManager.getGroupMemberEmails(gid)
                            val (driveFolderResult, errorMsg) = GoogleDriveVaultHelper.createVaultDriveFolder(context, event.name, false, memberEmails, dataManager, currentUid)
                            if (driveFolderResult == null) {
                                _uiState.update { it.copy(pendingVault = Pair(event.name, event.description)) }
                                event.onComplete(false, errorMsg ?: "Failed to create Google Drive folder.")
                                return@launch
                            }
                            val (folderId, folderUrl) = driveFolderResult
                            val newVault = Vault(
                                name = event.name,
                                description = event.description,
                                createdBy = settingsManager.getAppUserId(),
                                timestamp = System.currentTimeMillis(),
                                driveFolderId = folderId,
                                driveFolderUrl = folderUrl
                            )
                            val createdId = dataManager.addVault(gid, newVault)
                            _uiState.update { it.copy(pendingVault = null) }

                            val startTime = System.currentTimeMillis()
                            while (!_uiState.value.vaults.any { it.id == createdId || it.name == event.name } && System.currentTimeMillis() - startTime < 4000) {
                                delay(200)
                            }
                            event.onComplete(true, null)
                        } catch (e: Exception) {
                            event.onComplete(false, e.message ?: "Failed to create vault.")
                        }
                    } else {
                        event.onComplete(false, "User not authenticated")
                    }
                }
            }
            is VaultsEvent.UpdateVault -> {
                viewModelScope.launch {
                    dataManager.updateVault(gid, event.vault)
                }
            }
            is VaultsEvent.DeleteVault -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        val vault = _uiState.value.vaults.find { it.id == event.vaultId }
                        if (vault != null && vault.driveFolderId.isNotEmpty()) {
                            GoogleDriveVaultHelper.deleteRemoteDriveItem(context, vault.driveFolderId)
                        }
                        try {
                            val localVaultDir = File(context.filesDir, "vaults/${event.vaultId}")
                            if (localVaultDir.exists()) localVaultDir.deleteRecursively()
                        } catch (e: Exception) {
                            Log.e("DriveVault", "Failed local vault folder delete", e)
                        }
                        dataManager.deleteVault(gid, event.vaultId)
                        _uiState.update { state ->
                            val updatedVaults = state.vaults.filter { it.id != event.vaultId }
                            state.copy(
                                vaults = updatedVaults,
                                selectedVault = if (state.selectedVault?.id == event.vaultId) null else state.selectedVault,
                                docs = if (state.selectedVault?.id == event.vaultId) emptyList() else state.docs
                            )
                        }
                        event.onComplete(true)
                    } catch (e: Exception) {
                        Log.e("VaultsViewModel", "Failed to delete vault", e)
                        event.onComplete(false)
                    } finally {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
            is VaultsEvent.SelectVault -> selectVault(event.vault)
            is VaultsEvent.UploadDoc -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isUploading = true) }
                    try {
                        val memberEmails = dataManager.getGroupMemberEmails(gid)
                        
                        val uploadedFileIds = mutableListOf<String>()
                        val uploadedDriveUrls = mutableListOf<String>()
                        val fileNames = mutableListOf<String>()
                        val fileTypes = mutableListOf<String>()
                        
                        // Map localized string back to key if standard tag selected, otherwise it's custom.
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
                        val storedTag = standardTagsMap[event.tag.trim()] ?: event.tag.trim()
                        
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
                                // Save pending state and abort
                                _uiState.update {
                                    it.copy(
                                        pendingDoc = PendingDocUpload(
                                            vault = event.vault,
                                            files = event.files,
                                            docName = event.docName,
                                            tag = event.tag,
                                            docDate = event.docDate,
                                            description = event.description
                                        )
                                    )
                                }
                                event.onComplete(false, errorMsg ?: "Failed to upload document to Google Drive.")
                                return@launch
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
                            lastUpdatedBy = settingsManager.getAppUserId()
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
                                Log.e("VaultsViewModel", "Failed to cache files locally", e)
                            }
                        }

                        // Clean up temporary un-cached files in vault root
                        fileNames.forEach { fileName ->
                            val srcFile = File(context.filesDir, "vaults/${event.vault.id}/$fileName")
                            if (srcFile.exists()) {
                                srcFile.delete()
                            }
                        }

                        _uiState.update { it.copy(pendingDoc = null, offlineVersion = System.currentTimeMillis()) }
                        event.onComplete(true, null)
                    } catch (e: Exception) {
                        event.onComplete(false, e.message ?: "Failed to upload document.")
                    }
                }
            }
            is VaultsEvent.UpdateDoc -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(updatingDocIds = it.updatingDocIds + event.doc.id) }
                    try {
                        val memberEmails = dataManager.getGroupMemberEmails(gid)

                        val oldDoc = _uiState.value.docs.find { it.id == event.doc.id }
                        if (oldDoc != null && oldDoc.name != event.doc.name && event.doc.folderId.isNotEmpty()) {
                            try {
                                GoogleDriveVaultHelper.renameRemoteDriveItem(context, event.doc.folderId, event.doc.name)
                            } catch (e: Exception) {
                                Log.e("VaultsViewModel", "Failed to rename folder on Drive", e)
                            }
                        }

                        // 1. Delete removed Drive files and local cached files
                        event.removedDriveFileIds.filter { it.isNotEmpty() }.forEach { id ->
                            val index = oldDoc?.driveFileIds?.indexOf(id) ?: -1
                            if (index != -1 && oldDoc != null) {
                                VaultDocUtils.deleteLocalOfflineFile(context, oldDoc, index)
                            }
                            GoogleDriveVaultHelper.deleteRemoteDriveItem(context, id)
                        }

                        // 2. Upload new files
                        val newDriveFileIds = mutableListOf<String>()
                        val newDriveUrls = mutableListOf<String>()
                        val newFileNames = mutableListOf<String>()
                        val newFileTypes = mutableListOf<String>()

                        var docFolderId = event.doc.folderId
                        var docFolderUrl = event.doc.folderUrl
                        
                        if (docFolderId.isEmpty()) {
                            val (fId, fUrl) = GoogleDriveVaultHelper.createSubFolder(context, event.vault.driveFolderId, event.doc.name, memberEmails)
                            docFolderId = fId ?: ""
                            docFolderUrl = fUrl ?: ""
                        }
                        
                        val targetFolderId = if (docFolderId.isNotEmpty()) docFolderId else event.vault.driveFolderId

                        for (attachedFile in event.newFilesToUpload) {
                            val fullFileName = "${attachedFile.fileName}${attachedFile.extension}"
                            val (uploadResult, errorMsg) = GoogleDriveVaultHelper.uploadDocumentToDrive(
                                context, targetFolderId, attachedFile.uri, fullFileName, "document", memberEmails, event.vault.id
                            )
                            if (uploadResult != null) {
                                newDriveFileIds.add(uploadResult.first)
                                newDriveUrls.add(uploadResult.second)
                                newFileNames.add(fullFileName)
                                newFileTypes.add(attachedFile.extension.removePrefix("."))
                            } else {
                                event.onComplete(false, errorMsg ?: "Failed to upload file to Google Drive.")
                                return@launch
                            }
                        }

                        // 3. Map localized tag back to key if standard tag selected
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
                        val storedTag = standardTagsMap[event.doc.tag.trim()] ?: event.doc.tag.trim()

                        // 4. Combine existing kept files + new files
                        val finalDriveFileIds = event.doc.driveFileIds + newDriveFileIds
                        val finalDriveUrls = event.doc.driveUrls + newDriveUrls
                        val finalFileNames = event.doc.fileNames + newFileNames
                        val finalFileTypes = event.doc.fileTypes + newFileTypes

                        val updatedDoc = event.doc.copy(
                            tag = storedTag,
                            driveFileId = finalDriveFileIds.firstOrNull() ?: "",
                            driveUrl = finalDriveUrls.firstOrNull() ?: "",
                            fileType = finalFileTypes.firstOrNull() ?: "",
                            driveFileIds = finalDriveFileIds,
                            driveUrls = finalDriveUrls,
                            fileNames = finalFileNames,
                            fileTypes = finalFileTypes,
                            folderId = docFolderId,
                            folderUrl = docFolderUrl,
                            lastUpdatedBy = settingsManager.getAppUserId()
                        )

                        dataManager.updateVaultDoc(gid, event.doc.vaultId, updatedDoc)
                        if (_uiState.value.selectedDoc?.id == updatedDoc.id) {
                            _uiState.update { it.copy(selectedDoc = updatedDoc, offlineVersion = System.currentTimeMillis()) }
                        } else {
                            _uiState.update { it.copy(offlineVersion = System.currentTimeMillis()) }
                        }
                        event.onComplete(true, null)
                    } catch (e: Exception) {
                        event.onComplete(false, e.message ?: "Failed to update document.")
                    } finally {
                        _uiState.update { it.copy(updatingDocIds = it.updatingDocIds - event.doc.id) }
                    }
                }
            }
            is VaultsEvent.DeleteDoc -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(deletingDocIds = it.deletingDocIds + event.docId) }
                    try {
                        val doc = _uiState.value.docs.find { it.id == event.docId }
                        if (doc != null) {
                            if (doc.folderId.isNotEmpty()) {
                                GoogleDriveVaultHelper.deleteRemoteDriveItem(context, doc.folderId)
                            }
                            val driveIds = (doc.driveFileIds + doc.driveFileId + doc.effectiveDriveFileId).filter { it.isNotEmpty() && it != doc.folderId }.distinct()
                            driveIds.forEach { id ->
                                GoogleDriveVaultHelper.deleteRemoteDriveItem(context, id)
                            }
                            VaultDocUtils.deleteAllOfflineFiles(context, doc)
                        }
                        dataManager.deleteVaultDoc(gid, event.vaultId, event.docId)
                        event.onComplete(true)
                    } catch (e: Exception) {
                        Log.e("VaultsViewModel", "Failed to delete doc", e)
                        event.onComplete(false)
                    } finally {
                        _uiState.update { it.copy(deletingDocIds = it.deletingDocIds - event.docId, offlineVersion = System.currentTimeMillis()) }
                    }
                }
            }
            is VaultsEvent.DownloadDocForOffline -> {
                viewModelScope.launch {
                    try {
                        com.jtool.android.core.services.FileDownloadQueueManager.enqueue(context, event.doc)
                    } catch (e: Exception) {
                        Log.e("VaultsViewModel", "Failed offline download enqueue", e)
                    }
                }
            }
            is VaultsEvent.SaveToDownloads -> {
                viewModelScope.launch {
                    try {
                        com.jtool.android.core.services.FileExportQueueManager.enqueue(context, event.doc, event.fileIndex)
                        event.onComplete(true)
                    } catch (e: Exception) {
                        Log.e("VaultsViewModel", "Failed export to downloads enqueue", e)
                        event.onComplete(false)
                    }
                }
            }
            is VaultsEvent.DeleteDocOffline -> {
                viewModelScope.launch {
                    VaultDocUtils.deleteAllOfflineFiles(context, event.doc)
                    _uiState.update { it.copy(offlineVersion = System.currentTimeMillis()) }
                }
            }
            is VaultsEvent.DeleteSpecificFile -> {
                viewModelScope.launch {
                    try {
                        val fileId = event.doc.driveFileIds.getOrNull(event.fileIndex)
                        if (fileId != null && fileId.isNotEmpty()) {
                            GoogleDriveVaultHelper.deleteRemoteDriveItem(context, fileId)
                        }
                        
                        VaultDocUtils.deleteLocalOfflineFile(context, event.doc, event.fileIndex)
                        
                        val newFileIds = event.doc.driveFileIds.toMutableList().apply { if (size > event.fileIndex) removeAt(event.fileIndex) }
                        val newUrls = event.doc.driveUrls.toMutableList().apply { if (size > event.fileIndex) removeAt(event.fileIndex) }
                        val newNames = event.doc.fileNames.toMutableList().apply { if (size > event.fileIndex) removeAt(event.fileIndex) }
                        val newTypes = event.doc.fileTypes.toMutableList().apply { if (size > event.fileIndex) removeAt(event.fileIndex) }
                        
                        val updatedDoc = event.doc.copy(
                            driveFileIds = newFileIds,
                            driveUrls = newUrls,
                            fileNames = newNames,
                            fileTypes = newTypes,
                            driveFileId = newFileIds.firstOrNull() ?: "",
                            driveUrl = newUrls.firstOrNull() ?: "",
                            fileType = newTypes.firstOrNull() ?: "",
                            lastUpdatedBy = settingsManager.getAppUserId()
                        )
                        
                        dataManager.updateVaultDoc(gid, event.doc.vaultId, updatedDoc)
                        if (_uiState.value.selectedDoc?.id == updatedDoc.id) {
                            _uiState.update { it.copy(selectedDoc = updatedDoc, offlineVersion = System.currentTimeMillis()) }
                        } else {
                            _uiState.update { it.copy(offlineVersion = System.currentTimeMillis()) }
                        }
                    } catch (e: Exception) {
                        Log.e("DriveVault", "Failed to delete specific file", e)
                    }
                }
            }
            is VaultsEvent.SetFilterType -> _uiState.update { it.copy(filterType = event.filter) }
            is VaultsEvent.SetSortType -> _uiState.update { it.copy(sortType = event.sortType) }
            is VaultsEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.query) }
            is VaultsEvent.SetAdvancedFilters -> {
                _uiState.update {
                    it.copy(
                        nameFilter = event.name,
                        descFilter = event.desc,
                        tagFilter = event.tag,
                        downloadedOnly = event.downloadedOnly,
                        docDateFrom = event.docDateFrom,
                        docDateTo = event.docDateTo,
                        uploadDateFrom = event.uploadDateFrom,
                        uploadDateTo = event.uploadDateTo
                    )
                }
            }
            VaultsEvent.ClearAdvancedFilters -> {
                _uiState.update {
                    it.copy(
                        nameFilter = "",
                        descFilter = "",
                        tagFilter = "",
                        downloadedOnly = false,
                        docDateFrom = null,
                        docDateTo = null,
                        uploadDateFrom = null,
                        uploadDateTo = null,
                        searchQuery = ""
                    )
                }
            }
            VaultsEvent.RetryPendingAction -> {
                val state = _uiState.value
                val pVault = state.pendingVault
                val pDoc = state.pendingDoc
                if (pVault != null) {
                    onEvent(VaultsEvent.CreateVault(pVault.first, pVault.second))
                } else if (pDoc != null) {
                    onEvent(
                        VaultsEvent.UploadDoc(
                            vault = pDoc.vault,
                            files = pDoc.files,
                            docName = pDoc.docName,
                            tag = pDoc.tag,
                            docDate = pDoc.docDate,
                            description = pDoc.description
                        )
                    )
                }
            }
            VaultsEvent.Refresh -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        dataManager.fetchUserSettings(force = true)
                        observeVaults()
                        val currentVault = _uiState.value.selectedVault
                        if (currentVault != null) {
                            selectVault(currentVault)
                        }
                        delay(600)
                    } catch (e: Exception) {
                        Log.e("VaultsViewModel", "Failed to refresh", e)
                    } finally {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
            VaultsEvent.ReapplyPermissions -> {
                val currentVault = _uiState.value.selectedVault
                if (currentVault != null && gid != null) {
                    _uiState.update { it.copy(isLoading = true) }
                    viewModelScope.launch {
                        try {
                            val memberEmails = dataManager.getGroupMemberEmails(gid)
                            val wasUpdated = GoogleDriveVaultHelper.syncGroupFolderPermissions(context, currentVault.driveFolderId, memberEmails)
                        } catch (e: Exception) {
                            Log.e("VaultsViewModel", "Failed to reapply permissions", e)
                        } finally {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                }
            }
            is VaultsEvent.ValidateCachedData -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        val count = validateCachedData(gid)
                        event.onComplete(count)
                    } catch (e: Exception) {
                        Log.e("VaultsViewModel", "Failed cache validation", e)
                        event.onComplete(0)
                    } finally {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private suspend fun validateCachedData(gid: String, specificVaultId: String? = null): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                            val validFileNames = serverDoc.fileNames.toSet()
                            val docFiles = docEntry.listFiles() ?: emptyArray()
                            for (dFile in docFiles) {
                                if (validFileNames.isNotEmpty() && !validFileNames.contains(dFile.name)) {
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
            Log.e("VaultsViewModel", "Failed to validate cached data", e)
        }
        _uiState.update { it.copy(offlineVersion = System.currentTimeMillis()) }
        return@withContext removedCount
    }
}
