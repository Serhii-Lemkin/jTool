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
import com.jtool.android.features.vaults.common.VaultsOperationsHelper
import com.jtool.android.core.services.FileDownloadQueueManager
import com.google.firebase.auth.FirebaseAuth
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

        FileDownloadQueueManager.downloadState
            .onEach { _uiState.update { state -> state.copy(offlineVersion = System.currentTimeMillis()) } }
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
                        val (success, errorMsg) = VaultsOperationsHelper.performUploadDoc(
                            context = context,
                            dataManager = dataManager,
                            gid = gid,
                            currentUserId = settingsManager.getAppUserId(),
                            event = event
                        )
                        if (!success) {
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
                            event.onComplete(false, errorMsg)
                        } else {
                            _uiState.update { it.copy(pendingDoc = null, offlineVersion = System.currentTimeMillis()) }
                            event.onComplete(true, null)
                        }
                    } catch (e: Exception) {
                        event.onComplete(false, e.message ?: "Failed to upload document.")
                    } finally {
                        _uiState.update { it.copy(isUploading = false) }
                    }
                }
            }
            is VaultsEvent.UpdateDoc -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(updatingDocIds = it.updatingDocIds + event.doc.id) }
                    try {
                        val oldDoc = _uiState.value.docs.find { it.id == event.doc.id }
                        val currentUserId = settingsManager.getAppUserId()
                        val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val (updatedDoc, errorMsg) = VaultsOperationsHelper.performUpdateDoc(
                            context = context,
                            dataManager = dataManager,
                            gid = gid,
                            currentUserId = currentUserId,
                            authUid = authUid,
                            vault = event.vault,
                            eventDoc = event.doc,
                            oldDoc = oldDoc,
                            newFilesToUpload = event.newFilesToUpload,
                            removedDriveFileIds = event.removedDriveFileIds
                        )
                        if (updatedDoc != null) {
                            if (_uiState.value.selectedDoc?.id == updatedDoc.id) {
                                _uiState.update { it.copy(selectedDoc = updatedDoc, offlineVersion = System.currentTimeMillis()) }
                            } else {
                                _uiState.update { it.copy(offlineVersion = System.currentTimeMillis()) }
                            }
                            event.onComplete(true, null)
                        } else {
                            event.onComplete(false, errorMsg ?: "Failed to update document.")
                        }
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
                        val vault = _uiState.value.vaults.find { it.id == event.vaultId } ?: _uiState.value.selectedVault
                        val currentUserId = settingsManager.getAppUserId()
                        val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        if (doc != null && !VaultsOperationsHelper.isDocOrVaultOwner(doc, vault, currentUserId, authUid)) {
                            event.onComplete(false)
                            return@launch
                        }
                        VaultsOperationsHelper.performDeleteDoc(context, dataManager, gid, event.vaultId, event.docId, doc)
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
                        FileDownloadQueueManager.enqueue(context, event.doc)
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
                        val currentUserId = settingsManager.getAppUserId()
                        val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val vault = _uiState.value.vaults.find { it.id == event.doc.vaultId } ?: _uiState.value.selectedVault
                        if (!VaultsOperationsHelper.isDocOrVaultOwner(event.doc, vault, currentUserId, authUid)) {
                            return@launch
                        }
                        val updatedDoc = VaultsOperationsHelper.performDeleteSpecificFile(
                            context = context,
                            dataManager = dataManager,
                            gid = gid,
                            doc = event.doc,
                            fileIndex = event.fileIndex,
                            currentUserId = currentUserId
                        )
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

    private suspend fun validateCachedData(gid: String, specificVaultId: String? = null): Int {
        val removedCount = VaultsOperationsHelper.validateCachedData(context, dataManager, gid, specificVaultId)
        _uiState.update { it.copy(offlineVersion = System.currentTimeMillis()) }
        return removedCount
    }
}
