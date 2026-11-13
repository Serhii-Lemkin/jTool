package com.jtool.android.features.vaults

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.vaults.components.GroupMatchedDocListItem
import com.jtool.android.features.vaults.components.VaultListItemRow
import com.jtool.android.features.vaults.components.file_open_error_sheet.FileOpenErrorSheet
import com.jtool.android.features.vaults.components.floating_upload_progress.FloatingUploadProgressIndicator
import com.jtool.android.features.vaults.components.vaults_info_sheet.VaultsInfoSheet
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.enums.FileOpenErrorReasonEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.components.create_vault_sheet.CreateVaultSheet
import com.jtool.android.features.vaults.components.vault_delete_confirm_sheet.VaultDeleteConfirmSheet
import com.jtool.android.features.vaults.components.vault_delete_warning_sheet.VaultDeleteWarningSheet
import com.jtool.android.features.vaults.components.vault_item_action_sheet.VaultItemActionSheet
import com.jtool.android.features.vaults.components.vaults_list_settings_sheet.VaultsListSettingsSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
import kotlinx.coroutines.launch

enum class ActiveSearchTarget { NONE, VAULTS_ONLY, DOCS_ACROSS_VAULTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    dataManager: DataManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    isAdmin: Boolean,
    currentGroupId: String?,
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: VaultsViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = remember { settingsManager.getAppUserId() }
    val authUid = remember { authManager.getCurrentUser()?.uid ?: "" }
    val showDriveButtons = remember { settingsManager.isShowDriveButtonsEnabled() }
    val isOnline = remember { VaultDocUtils.isNetworkAvailable(context) }

    var showCreateSheet by remember { mutableStateOf(false) }
    var editingVault by remember { mutableStateOf<Vault?>(null) }
    var vaultForActionSheet by remember { mutableStateOf<Vault?>(null) }
    
    var warningDeleteVault by remember { mutableStateOf<Vault?>(null) }
    var confirmDeleteVault by remember { mutableStateOf<Vault?>(null) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }
    var openErrorReason by remember { mutableStateOf<FileOpenErrorReasonEnum?>(null) }
    var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var searchTarget by remember { mutableStateOf(ActiveSearchTarget.NONE) }

    val hasDocFilter = remember(uiState) {
        uiState.nameFilter.isNotBlank() || uiState.descFilter.isNotBlank() || uiState.tagFilter.isNotBlank() ||
        uiState.downloadedOnly || uiState.docDateFrom != null || uiState.docDateTo != null ||
        uiState.uploadDateFrom != null || uiState.uploadDateTo != null || uiState.sortType != VaultSortEnum.UPLOAD_DATE_DESC
    }

    var pinnedVaultIds by remember { mutableStateOf(settingsManager.getPinnedVaultIds()) }

    val pagedVaults = remember(uiState.filteredVaults, uiState.pagingLimit, hasDocFilter, pinnedVaultIds) {
        if (hasDocFilter) {
            emptyList()
        } else {
            val (pinned, unpinned) = uiState.filteredVaults.partition { pinnedVaultIds.contains(it.id) }
            val sortedPinned = pinned.sortedBy { pinnedVaultIds.indexOf(it.id) }
            (sortedPinned + unpinned).take(uiState.pagingLimit)
        }
    }

    LaunchedEffect(uiState.searchQuery) { draftSearchQuery = uiState.searchQuery }
    LaunchedEffect(currentGroupId) { viewModel.setGroupId(currentGroupId) }

    var showVaultsInfoSheet by remember { mutableStateOf(false) }
    var showVaultsListSettingsSheet by remember { mutableStateOf(false) }
    var showSearchOptionSheet by remember { mutableStateOf(false) }
    var showDocFilterSheet by remember { mutableStateOf(false) }

    val allGroupDocs by viewModel.allGroupDocs.collectAsState()
    val uploadState by com.jtool.android.core.services.FileUploadQueueManager.uploadState.collectAsState()
    val downloadState by com.jtool.android.core.services.FileDownloadQueueManager.downloadState.collectAsState()

    val filteredGroupDocs = remember(allGroupDocs, uiState, uploadState, downloadState) {
        val qName = uiState.nameFilter.trim().lowercase()
        val qDesc = uiState.descFilter.trim().lowercase()
        val qTag = uiState.tagFilter.trim().lowercase()
        val isHasFilter = qName.isNotEmpty() || qDesc.isNotEmpty() || qTag.isNotEmpty() ||
                uiState.downloadedOnly || uiState.docDateFrom != null || uiState.docDateTo != null ||
                uiState.uploadDateFrom != null || uiState.uploadDateTo != null

        if (!isHasFilter) emptyList()
        else {
            val matched = allGroupDocs.filter { doc ->
                val matchesName = qName.isEmpty() || doc.name.lowercase().contains(qName)
                val matchesDesc = qDesc.isEmpty() || doc.description.lowercase().contains(qDesc)
                val matchesTag = qTag.isEmpty() || doc.tag.lowercase().contains(qTag)
                val isDownloaded = VaultDocUtils.isDocumentDownloadedLocally(context, doc)
                val matchesDownloaded = !uiState.downloadedOnly || isDownloaded
                
                val docDate = doc.documentTimestamp
                val matchesDocDate = (uiState.docDateFrom == null || docDate >= uiState.docDateFrom!!) && (uiState.docDateTo == null || docDate <= uiState.docDateTo!!)
                
                val upDate = doc.uploadTimestamp
                val matchesUpDate = (uiState.uploadDateFrom == null || upDate >= uiState.uploadDateFrom!!) && (uiState.uploadDateTo == null || upDate <= uiState.uploadDateTo!!)

                matchesName && matchesDesc && matchesTag && matchesDownloaded && matchesDocDate && matchesUpDate
            }

            when (uiState.sortType) {
                VaultSortEnum.UPLOAD_DATE_DESC -> matched.sortedByDescending { it.uploadTimestamp }
                VaultSortEnum.NAME_ASC -> matched.sortedBy { it.name.lowercase() }
                VaultSortEnum.DOC_DATE_DESC -> matched.sortedByDescending { it.documentTimestamp }
                VaultSortEnum.TAG_ASC -> matched.sortedBy { it.tag.lowercase() }
            }
        }
    }

    val pagedGroupDocs = remember(filteredGroupDocs, uiState.pagingLimit) {
        filteredGroupDocs.take(uiState.pagingLimit)
    }

    if (showSearchOptionSheet) {
        com.jtool.android.features.vaults.components.vaults_search_option_sheet.VaultsSearchOptionSheet(
            onSelectMode = { mode ->
                when (mode) {
                    com.jtool.android.features.vaults.components.vaults_search_option_sheet.VaultSearchMode.VAULTS_ONLY -> {
                        searchTarget = ActiveSearchTarget.VAULTS_ONLY
                        viewModel.onEvent(VaultsEvent.ClearAdvancedFilters)
                        viewModel.onEvent(VaultsEvent.ToggleSearch)
                    }
                    com.jtool.android.features.vaults.components.vaults_search_option_sheet.VaultSearchMode.DOCS_ACROSS_VAULTS -> {
                        searchTarget = ActiveSearchTarget.DOCS_ACROSS_VAULTS
                        viewModel.onEvent(VaultsEvent.UpdateSearchQuery(""))
                        showDocFilterSheet = true
                    }
                }
            },
            onDismiss = { showSearchOptionSheet = false }
        )
    }

    if (showDocFilterSheet) {
        com.jtool.android.features.vaults.components.doc_filter_sheet.DocFilterSheet(
            nameQuery = uiState.nameFilter,
            descQuery = uiState.descFilter,
            tagQuery = uiState.tagFilter,
            downloadedOnly = uiState.downloadedOnly,
            docDateFrom = uiState.docDateFrom,
            docDateTo = uiState.docDateTo,
            uploadDateFrom = uiState.uploadDateFrom,
            uploadDateTo = uiState.uploadDateTo,
            currentSort = uiState.sortType,
            onApplyFilters = { name, desc, tag, downloadedOnly, ddf, ddt, udf, udt, sort ->
                searchTarget = ActiveSearchTarget.DOCS_ACROSS_VAULTS
                viewModel.onEvent(
                    VaultsEvent.SetAdvancedFilters(
                        name, desc, tag, downloadedOnly, ddf, ddt, udf, udt
                    )
                )
                viewModel.onEvent(VaultsEvent.SetSortType(sort))
            },
            onClearFilters = {
                searchTarget = ActiveSearchTarget.NONE
                viewModel.onEvent(VaultsEvent.ClearAdvancedFilters)
                viewModel.onEvent(VaultsEvent.SetSortType(VaultSortEnum.UPLOAD_DATE_DESC))
            },
            onDismiss = { showDocFilterSheet = false }
        )
    }

    LaunchedEffect(Unit) {
        val uid = authManager.getCurrentUser()?.uid
        if (uid != null) {
            val seenInfo = settingsManager.getSeenVaultsInfo()
            if (!seenInfo) {
                showVaultsInfoSheet = true
                viewModel.markVaultsInfoSeen(uid)
            }
        }
    }

    if (showVaultsInfoSheet) {
        VaultsInfoSheet(onDismiss = { showVaultsInfoSheet = false })
    }

    if (showVaultsListSettingsSheet) {
        VaultsListSettingsSheet(
            onVaultSettings = { onNavigate(AppScreenEnum.VAULT_SETTINGS) },
            onVaultsInfo = { showVaultsInfoSheet = true },
            onLearnMore = { com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "vaults") },
            onDismiss = { showVaultsListSettingsSheet = false }
        )
    }

    BaseScreen(
        title = Loc.Vaults.vaults,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(VaultsEvent.Refresh) },
        isSearchVisible = uiState.isSearchVisible,
        searchQuery = draftSearchQuery,
        onSearchQueryChange = { draftSearchQuery = it },
        onSearchClose = {
            draftSearchQuery = ""
            searchTarget = ActiveSearchTarget.NONE
            viewModel.onEvent(VaultsEvent.UpdateSearchQuery(""))
            viewModel.onEvent(VaultsEvent.ClearAdvancedFilters)
            viewModel.onEvent(VaultsEvent.ToggleSearch)
        },
        onSearchTriggered = {
            viewModel.onEvent(VaultsEvent.UpdateSearchQuery(draftSearchQuery))
        },
        actions = {
            IconButton(onClick = { viewModel.onEvent(VaultsEvent.ToggleSearch) }) {
                Icon(Icons.Default.Search, contentDescription = "Search Vaults")
            }
            IconButton(onClick = { showVaultsListSettingsSheet = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { showDocFilterSheet = true },
                    containerColor = if (hasDocFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (hasDocFilter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Default.FindInPage,
                            contentDescription = Loc.s("vaults_searchDocs", "Search Documents"),
                            modifier = Modifier.size(24.dp)
                        )
                        if (hasDocFilter) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }

                UnifiedFAB(
                    onClick = { showCreateSheet = true },
                    contentDescription = Loc.Vaults.createVault
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (!uiState.isLoading && pagedVaults.isEmpty() && pagedGroupDocs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (draftSearchQuery.isNotBlank() || hasDocFilter) {
                            Loc.s("vaults_noVaultsOrDocs", "No matching vaults or documents found")
                        } else Loc.Vaults.noVaults,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (!uiState.isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pagedGroupDocs.isNotEmpty()) {
                        item {
                            Text(
                                text = Loc.s("vaults_matchingDocsGroup", "Matching Documents Across Group Vaults"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(pagedGroupDocs, key = { "group_doc_${it.id}" }) { doc ->
                            val vault = uiState.vaults.find { it.id == doc.vaultId }
                            GroupMatchedDocListItem(
                                doc = doc,
                                vault = vault,
                                uiState = uiState,
                                uploadState = uploadState,
                                onUpdatingBlocked = {
                                    infoMessage = Loc.s("vaults_docUpdatingMessage", "Record is being updated. Please wait for the background update to finish.") to MessageType.INFO
                                },
                                onOpenDetails = {
                                    if (vault != null) {
                                        viewModel.onEvent(VaultsEvent.SelectVault(vault))
                                        viewModel.selectDoc(doc)
                                        onNavigate(AppScreenEnum.VAULT_DOC_DETAILS)
                                    }
                                }
                            )
                        }

                        if (filteredGroupDocs.size > pagedGroupDocs.size) {
                            item {
                                TextButton(
                                    onClick = { viewModel.onEvent(VaultsEvent.LoadMore) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Text(Loc.s("vaults_loadMoreDocs", "Load More Documents"))
                                }
                            }
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }

                    if (pagedVaults.isNotEmpty()) {
                        items(pagedVaults, key = { it.id }) { vault ->
                            val ownerName = uiState.userNames[vault.createdBy] ?: Loc.General.unknown
                            val isOwner = vault.createdBy.isEmpty() || vault.createdBy == currentUserId || vault.createdBy == authUid || isAdmin
                            val isPinned = pinnedVaultIds.contains(vault.id)

                            VaultListItemRow(
                                vault = vault,
                                ownerName = ownerName,
                                isOwner = isOwner,
                                isPinned = isPinned,
                                showDriveButtons = showDriveButtons,
                                onClick = {
                                    viewModel.onEvent(VaultsEvent.SelectVault(vault))
                                    onNavigate(AppScreenEnum.VAULT_DETAILS)
                                },
                                onMoreClick = { vaultForActionSheet = vault },
                                onOpenError = { openErrorReason = it }
                            )
                        }

                        if (uiState.filteredVaults.size > pagedVaults.size) {
                            item {
                                TextButton(
                                    onClick = {
                                        if (!isOnline) {
                                            infoMessage = Loc.s("vaults_offlineLoadError", "No internet connection. Connection required to load more vaults.") to MessageType.INFO
                                        } else {
                                            viewModel.onEvent(VaultsEvent.LoadMore)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Text(Loc.s("vaults_loadMoreVaults", "Load More Vaults"))
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (vaultForActionSheet != null) {
        VaultItemActionSheet(
            vault = vaultForActionSheet!!,
            isPinned = pinnedVaultIds.contains(vaultForActionSheet!!.id),
            onTogglePin = {
                settingsManager.togglePinVault(vaultForActionSheet!!.id)
                pinnedVaultIds = settingsManager.getPinnedVaultIds()
            },
            onEdit = {
                editingVault = vaultForActionSheet
                vaultForActionSheet = null
            },
            onDelete = {
                warningDeleteVault = vaultForActionSheet
                vaultForActionSheet = null
            },
            onDismiss = { vaultForActionSheet = null }
        )
    }

    if (showCreateSheet) {
        CreateVaultSheet(
            onDismiss = { showCreateSheet = false },
            onConfirm = { name, desc ->
                viewModel.onEvent(VaultsEvent.CreateVault(name, desc) { success, errorMsg ->
                    if (!success) {
                        infoMessage = (errorMsg ?: Loc.Vaults.driveFolderError) to MessageType.ERROR
                    } else {
                        showCreateSheet = false
                    }
                })
            }
        )
    }

    if (editingVault != null) {
        CreateVaultSheet(
            editingVault = editingVault,
            onDismiss = { editingVault = null },
            onConfirm = { name, desc ->
                viewModel.onEvent(VaultsEvent.UpdateVault(editingVault!!.copy(name = name, description = desc)))
            }
        )
    }

    if (warningDeleteVault != null) {
        VaultDeleteWarningSheet(
            vault = warningDeleteVault!!,
            onDismiss = { warningDeleteVault = null },
            onProceedToConfirm = {
                confirmDeleteVault = warningDeleteVault
                warningDeleteVault = null
            }
        )
    }

    if (confirmDeleteVault != null) {
        VaultDeleteConfirmSheet(
            vault = confirmDeleteVault!!,
            onDismiss = { confirmDeleteVault = null },
            onConfirmDelete = {
                val v = confirmDeleteVault
                confirmDeleteVault = null
                if (v != null) {
                    viewModel.onEvent(VaultsEvent.DeleteVault(v.id) { success ->
                        if (success) {
                            infoMessage = "Vault Deleted" to MessageType.SUCCESS
                        } else {
                            infoMessage = "Failed to delete vault." to MessageType.ERROR
                        }
                    })
                }
            }
        )
    }

    if (infoMessage != null) {
        MessageSheet(
            message = infoMessage!!.first,
            type = infoMessage!!.second,
            onDismiss = { infoMessage = null }
        )
    }

    if (openErrorReason != null) {
        FileOpenErrorSheet(
            reason = openErrorReason!!,
            onDismiss = { openErrorReason = null }
        )
    }
}
