package com.jtool.android.features.vaults

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.common.DocIconWithBadge
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.components.delete_local_file_confirm_sheet.DeleteLocalFileConfirmSheet
import com.jtool.android.core.services.FileDownloadQueueManager
import com.jtool.android.core.services.FileUploadQueueManager
import com.jtool.android.features.vaults.components.doc_delete_confirm_sheet.DocDeleteConfirmSheet
import com.jtool.android.features.vaults.components.doc_files_sheet.DocFilesSheet
import com.jtool.android.features.vaults.components.doc_filter_sheet.DocFilterSheet
import com.jtool.android.features.vaults.components.doc_item_action_sheet.DocItemActionSheet
import com.jtool.android.features.vaults.components.edit_doc_sheet.EditDocSheet
import com.jtool.android.features.vaults.components.floating_upload_progress.FloatingUploadProgressIndicator
import com.jtool.android.features.vaults.components.upload_doc_sheet.UploadDocSheet
import com.jtool.android.features.vaults.components.vault_permissions_sheet.VaultPermissionsSheet
import com.jtool.android.features.vaults.components.vault_settings_menu_sheet.VaultSettingsMenuSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDetailsScreen(
    dataManager: DataManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: VaultsViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val vault = uiState.selectedVault ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val currentUserId = remember { settingsManager.getAppUserId() }
    val authUid = remember { authManager.getCurrentUser()?.uid ?: "" }
    val isVaultOwner = vault.createdBy == currentUserId || vault.createdBy == authUid || isAdmin
    val showDriveButtons = remember { settingsManager.isShowDriveButtonsEnabled() }

    val currentGroupId = remember { settingsManager.getCurrentGroupId() }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showUploadSheet by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showVaultSettingsMenuSheet by remember { mutableStateOf(false) }
    var groupMemberEmails by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(vault.id, currentGroupId) {
        if (currentGroupId != null) {
            groupMemberEmails = dataManager.getGroupMemberEmails(currentGroupId)
        }
    }
    var docToEdit by remember { mutableStateOf<VaultDocument?>(null) }
    var docToDelete by remember { mutableStateOf<VaultDocument?>(null) }
    var docToDeleteOffline by remember { mutableStateOf<VaultDocument?>(null) }
    var docForFilesSheet by remember { mutableStateOf<VaultDocument?>(null) }
    var docForActionSheet by remember { mutableStateOf<Pair<VaultDocument, Boolean>?>(null) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }
    var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }

    val uploadState by com.jtool.android.core.services.FileUploadQueueManager.uploadState.collectAsState()
    val downloadState by FileDownloadQueueManager.downloadState.collectAsState()

    val isOnline = remember { VaultDocUtils.isNetworkAvailable(context) }
    val displayedDocs = remember(uiState.sortedAndFilteredDocs, isOnline, uiState.offlineVersion, uploadState, downloadState) {
        if (!isOnline) {
            uiState.sortedAndFilteredDocs.filter { VaultDocUtils.isDocumentDownloadedLocally(context, it) }
        } else {
            uiState.sortedAndFilteredDocs
        }
    }
    val pagedDocs = remember(displayedDocs, uiState.pagingLimit) {
        displayedDocs.take(uiState.pagingLimit)
    }

    val hasDocFilter = remember(uiState) {
        uiState.nameFilter.isNotBlank() || uiState.descFilter.isNotBlank() || uiState.tagFilter.isNotBlank() ||
        uiState.downloadedOnly || uiState.docDateFrom != null || uiState.docDateTo != null ||
        uiState.uploadDateFrom != null || uiState.uploadDateTo != null || uiState.sortType != VaultSortEnum.UPLOAD_DATE_DESC
    }

    BaseScreen(
        title = vault.name,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(VaultsEvent.Refresh) },
        isSearchVisible = uiState.isSearchVisible,
        searchQuery = draftSearchQuery,
        onSearchQueryChange = {
            draftSearchQuery = it
            viewModel.onEvent(VaultsEvent.UpdateSearchQuery(it))
        },
        onSearchClose = {
            draftSearchQuery = ""
            viewModel.onEvent(VaultsEvent.UpdateSearchQuery(""))
            viewModel.onEvent(VaultsEvent.ToggleSearch)
        },
        actions = {
            IconButton(onClick = { showVaultSettingsMenuSheet = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { showFilterSheet = true },
                    containerColor = if (hasDocFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (hasDocFilter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape
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
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }

                UnifiedFAB(
                    onClick = { showUploadSheet = true },
                    contentDescription = Loc.Vaults.uploadDocument
                )
            }
        }
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.onEvent(VaultsEvent.Refresh) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    // Keep empty while loading so loader, items, and FAB reveal simultaneously
                } else if (displayedDocs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (!isOnline) "No offline cached documents available" else Loc.Vaults.noDocs,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pagedDocs, key = { it.id }) { doc ->
                            val dateFormat = remember(context) { com.jtool.android.core.utils.getPreferredSimpleDateFormat(context) }
                            val dateStr = dateFormat.format(Date(doc.documentTimestamp))
                            val isDocOwner = doc.lastUpdatedBy == currentUserId || doc.lastUpdatedBy == authUid || isVaultOwner
                            val isDownloaded = remember(doc.id, uiState.offlineVersion, downloadState) {
                                VaultDocUtils.isDocumentDownloadedLocally(context, doc)
                            }
                            val isDownloading = uiState.downloadingDocIds.contains(doc.id)
                            val isDeleting = uiState.deletingDocIds.contains(doc.id)
                            val firstFileName = doc.fileNames.firstOrNull() ?: doc.name
                            val fileCount = if (doc.fileNames.isNotEmpty()) doc.fileNames.size else if (doc.driveFileIds.isNotEmpty()) doc.driveFileIds.size else 1
                            val tagText = if (doc.tag.isNotBlank()) "${VaultDocUtils.getLocalizedTag(doc.tag)} • " else ""

                            val isUpdating = remember(doc.id, uiState.updatingDocIds, uploadState) {
                                uiState.updatingDocIds.contains(doc.id) ||
                                uploadState.tasks.any { !it.isFailed && it.existingDocId == doc.id && it.files.any { f -> f.status != com.jtool.android.core.services.UploadFileStatus.COMPLETED } }
                            }

                            val metadataStr = when {
                                isDeleting -> Loc.s("general_deleting", "Deleting...")
                                isUpdating -> Loc.s("vaults_updatingBg", "Updating in background...")
                                isDownloading -> Loc.General.downloading
                                isDownloaded -> Loc.Vaults.downloaded
                                else -> ""
                            }

                            val isSavingToDownloads = uiState.savingToDownloadsDocIds.contains(doc.id)

                            ActionListItem(
                                onClick = {
                                    if (isUpdating) {
                                        infoMessage = Loc.s("vaults_docUpdatingMessage", "Record is being updated. Please wait for the background update to finish.") to MessageType.INFO
                                    } else if (!isDeleting) {
                                        docForFilesSheet = doc
                                    }
                                },
                                headline = doc.name,
                                supportingComponent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (doc.description.isNotBlank()) {
                                            Text(
                                                text = doc.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (tagText.isNotEmpty()) {
                                                Text(
                                                    text = tagText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            Text(
                                                text = "$fileCount ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (metadataStr.isNotEmpty()) {
                                            Text(
                                                text = metadataStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                },
                                leadingContent = {
                                    DocIconWithBadge(
                                        fileName = firstFileName,
                                        isDownloaded = isDownloaded,
                                        isDownloading = isDownloading || isUpdating,
                                        isDeleting = isDeleting,
                                        isSavingToDownloads = isSavingToDownloads
                                    )
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (isUpdating) {
                                                    infoMessage = Loc.s("vaults_docUpdatingMessage", "Record is being updated. Please wait for the background update to finish.") to MessageType.INFO
                                                } else {
                                                    viewModel.selectDoc(doc)
                                                    onNavigate(AppScreenEnum.VAULT_DOC_DETAILS)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = "Doc Details")
                                        }

                                        if (isDocOwner && !isDeleting) {
                                            IconButton(
                                                onClick = {
                                                    if (isUpdating) {
                                                        infoMessage = Loc.s("vaults_docUpdatingMessage", "Record is being updated. Please wait for the background update to finish.") to MessageType.INFO
                                                    } else {
                                                        docForActionSheet = doc to isDownloaded
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.MoreVert, null)
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        if (displayedDocs.size > pagedDocs.size) {
                            item {
                                TextButton(
                                    onClick = { viewModel.onEvent(VaultsEvent.LoadMore) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Text(Loc.s("vaults_loadMore", "Load More"))
                                }
                            }
                        } else if (!isOnline && uiState.filteredDocs.size > displayedDocs.size) {
                            item {
                                TextButton(
                                    onClick = {
                                        infoMessage = Loc.s("vaults_offlineViewError", "No internet connection. Connection required to view uncached online documents.") to MessageType.INFO
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Text(Loc.s("vaults_loadMoreOffline", "Load More (Offline)"))
                                }
                            }
                        }

                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }

    if (docForFilesSheet != null) {
        DocFilesSheet(
            doc = docForFilesSheet!!,
            offlineVersion = uiState.offlineVersion,
            onDismiss = { docForFilesSheet = null }
        )
    }

    if (docForActionSheet != null) {
        val (selectedDoc, isDocDownloaded) = docForActionSheet!!
        val isDocOwner = selectedDoc.lastUpdatedBy == currentUserId || selectedDoc.lastUpdatedBy == authUid || isVaultOwner
        DocItemActionSheet(
            doc = selectedDoc,
            isDownloaded = isDocDownloaded,
            isDocOwner = isDocOwner,
            showDriveButtons = showDriveButtons,
            onOpenDrive = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedDoc.effectiveDriveUrl))
                context.startActivity(intent)
            },
            onOpenFolder = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedDoc.folderUrl))
                context.startActivity(intent)
            },
            onDownloadOffline = {
                viewModel.onEvent(VaultsEvent.DownloadDocForOffline(selectedDoc))
            },
            onRemoveOffline = {
                docToDeleteOffline = selectedDoc
            },
            onSaveToDownloads = {
                viewModel.onEvent(VaultsEvent.SaveToDownloads(selectedDoc) { success ->
                    if (success) {
                        infoMessage = Loc.Vaults.savedToDownloads to MessageType.SUCCESS
                    } else {
                        infoMessage = "Failed to save file to Downloads" to MessageType.ERROR
                    }
                })
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${selectedDoc.name}\n${selectedDoc.effectiveDriveUrl}")
                }
                context.startActivity(Intent.createChooser(intent, Loc.General.share))
            },
            onEdit = {
                docToEdit = selectedDoc
            },
            onDelete = {
                docToDelete = selectedDoc
            },
            onDismiss = { docForActionSheet = null }
        )
    }

    if (showPermissionsSheet) {
        VaultPermissionsSheet(
            vault = vault,
            groupMemberEmails = groupMemberEmails,
            userNames = uiState.userNames,
            onDismiss = { showPermissionsSheet = false }
        )
    }

    if (showVaultSettingsMenuSheet) {
        VaultSettingsMenuSheet(
            isVaultOwner = isVaultOwner,
            onCheckPermissions = { showPermissionsSheet = true },
            onReapplyPermissions = {
                viewModel.onEvent(VaultsEvent.ReapplyPermissions)
                infoMessage = VaultsLoc.reapplyPermissionsSuccess to MessageType.SUCCESS
            },
            onValidateCachedData = {
                viewModel.onEvent(VaultsEvent.ValidateCachedData {
                    infoMessage = VaultsLoc.validateVaultsSuccess to MessageType.SUCCESS
                })
            },
            onDismiss = { showVaultSettingsMenuSheet = false }
        )
    }

    if (showFilterSheet) {
        DocFilterSheet(
            nameQuery = uiState.nameFilter,
            descQuery = uiState.descFilter,
            tagQuery = uiState.tagFilter,
            downloadedOnly = uiState.downloadedOnly,
            docDateFrom = uiState.docDateFrom,
            docDateTo = uiState.docDateTo,
            uploadDateFrom = uiState.uploadDateFrom,
            uploadDateTo = uiState.uploadDateTo,
            currentSort = uiState.sortType,
            onApplyFilters = { name, desc, tag, downloadedOnly, dDF, dDT, uDF, uDT, sort ->
                viewModel.onEvent(VaultsEvent.SetAdvancedFilters(name, desc, tag, downloadedOnly, dDF, dDT, uDF, uDT))
                viewModel.onEvent(VaultsEvent.SetSortType(sort))
            },
            onClearFilters = {
                viewModel.onEvent(VaultsEvent.ClearAdvancedFilters)
                viewModel.onEvent(VaultsEvent.SetSortType(VaultSortEnum.UPLOAD_DATE_DESC))
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showUploadSheet) {
        UploadDocSheet(
            vault = vault,
            isUploading = uiState.isUploading,
            onDismiss = { showUploadSheet = false },
            onUpload = { files, docName, tag, docDate, desc, saveOffline ->
                showUploadSheet = false
                FileUploadQueueManager.enqueue(context, vault, files, docName, tag, docDate, desc, saveOffline)
                infoMessage = "Uploading files in background... The document will appear in Vaults once complete." to MessageType.INFO
            }
        )
    }

    if (docToEdit != null) {
        EditDocSheet(
            vault = vault,
            doc = docToEdit!!,
            isUploading = uiState.isUploading,
            onDismiss = { docToEdit = null },
            onConfirm = { updatedDoc, newFiles, removedIds ->
                docToEdit = null
                if (newFiles.isNotEmpty()) {
                    com.jtool.android.core.services.FileUploadQueueManager.enqueue(
                        context = context,
                        vault = vault,
                        files = newFiles,
                        docName = updatedDoc.name,
                        tag = updatedDoc.tag,
                        docDate = updatedDoc.documentTimestamp,
                        description = updatedDoc.description,
                        saveOffline = false,
                        existingDocId = updatedDoc.id,
                        existingDriveFileIds = updatedDoc.driveFileIds,
                        existingDriveUrls = updatedDoc.driveUrls,
                        existingFileNames = updatedDoc.fileNames,
                        existingFileTypes = updatedDoc.fileTypes,
                        folderId = updatedDoc.folderId,
                        folderUrl = updatedDoc.folderUrl
                    )
                    infoMessage = "Uploading new files in background..." to MessageType.INFO
                }
                if (removedIds.isNotEmpty() || newFiles.isEmpty()) {
                    viewModel.onEvent(
                        VaultsEvent.UpdateDoc(
                            vault = vault,
                            doc = updatedDoc,
                            newFilesToUpload = emptyList(),
                            removedDriveFileIds = removedIds
                        ) { success, errorMsg ->
                            if (!success) {
                                infoMessage = (errorMsg ?: Loc.Vaults.driveUploadError) to MessageType.ERROR
                            }
                        }
                    )
                }
            }
        )
    }

    if (docToDelete != null) {
        DocDeleteConfirmSheet(
            doc = docToDelete!!,
            onDismiss = { docToDelete = null },
            onConfirm = {
                val d = docToDelete!!
                docToDelete = null
                viewModel.onEvent(VaultsEvent.DeleteDoc(vault.id, d.id) { success ->
                    if (success) {
                        infoMessage = "Document Deleted" to MessageType.SUCCESS
                    } else {
                        infoMessage = "Failed to delete document" to MessageType.ERROR
                    }
                })
            }
        )
    }

    if (docToDeleteOffline != null) {
        DeleteLocalFileConfirmSheet(
            doc = docToDeleteOffline!!,
            onDismiss = { docToDeleteOffline = null },
            onConfirmDeleteLocal = {
                viewModel.onEvent(VaultsEvent.DeleteDocOffline(docToDeleteOffline!!))
                docToDeleteOffline = null
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
}
