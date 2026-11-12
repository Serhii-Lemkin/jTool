package com.jtool.android.features.vaults.doc

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.services.FileDownloadQueueManager
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.VaultsEvent
import com.jtool.android.features.vaults.VaultsViewModel
import com.jtool.android.features.vaults.common.DocIconWithBadge
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.components.delete_local_file_confirm_sheet.DeleteLocalFileConfirmSheet
import com.jtool.android.features.vaults.components.download_confirm_sheet.DownloadConfirmSheet
import com.jtool.android.features.vaults.components.doc_delete_confirm_sheet.DocDeleteConfirmSheet
import com.jtool.android.features.vaults.components.file_action_sheet.FileActionSheet
import com.jtool.android.features.vaults.components.edit_doc_sheet.EditDocSheet
import com.jtool.android.features.vaults.components.save_to_downloads_info_sheet.SaveToDownloadsInfoSheet
import com.jtool.android.features.vaults.components.share_doc_sheet.ShareDocSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocDetailsScreen(
    dataManager: DataManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    isAdmin: Boolean,
    onBack: () -> Unit,
    viewModel: VaultsViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val doc = uiState.selectedDoc ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val currentUserId = remember { settingsManager.getAppUserId() }
    val authUid = remember { authManager.getCurrentUser()?.uid ?: "" }
    val scope = rememberCoroutineScope()
    val showDriveButtons = remember { settingsManager.isShowDriveButtonsEnabled() }
    val isVaultOwner = uiState.selectedVault?.createdBy == currentUserId || uiState.selectedVault?.createdBy == authUid || isAdmin
    val isDocOwner = doc.lastUpdatedBy == currentUserId || doc.lastUpdatedBy == authUid || isVaultOwner

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteSheet by remember { mutableStateOf(false) }
    var showDeleteOfflineSheet by remember { mutableStateOf(false) }
    var showDownloadConfirmSheet by remember { mutableStateOf(false) }
    var showSaveToDownloadsSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var selectedFileIndexForSheet by remember { mutableStateOf<Int?>(null) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }

    val dateFormat = remember(context) { com.jtool.android.core.utils.getPreferredSimpleDateFormat(context) }
    val downloadState by FileDownloadQueueManager.downloadState.collectAsState()
    val uploadState by com.jtool.android.core.services.FileUploadQueueManager.uploadState.collectAsState()
    val isUpdating = remember(doc.id, uiState.updatingDocIds, uploadState) {
        uiState.updatingDocIds.contains(doc.id) ||
        uploadState.tasks.any { !it.isFailed && it.existingDocId == doc.id && it.files.any { f -> f.status != com.jtool.android.core.services.UploadFileStatus.COMPLETED } }
    }
    val datePattern = remember(context) { com.jtool.android.core.utils.getPreferredDateFormatPattern(context) }
    val uploadDateFormat = remember(datePattern) { SimpleDateFormat("$datePattern HH:mm", Locale.getDefault()) }
    val isDownloaded = remember(doc.id, uiState.offlineVersion, downloadState) {
        VaultDocUtils.isDocumentDownloadedLocally(context, doc)
    }
    val localFile = remember(isDownloaded) { VaultDocUtils.getOfflineLocalFile(context, doc) }

    if (isUpdating) {
        BaseScreen(
            title = doc.name,
            onBack = onBack
        ) { padding ->
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = Loc.s("vaults_docUpdatingMessage", "Record is being updated. Please wait for the background update to finish."),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
        return
    }

    BaseScreen(
        title = Loc.Vaults.document,
        isLoading = false,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(VaultsEvent.Refresh) },
        actions = {
            IconButton(onClick = { showEditSheet = true }) {
                Icon(Icons.Default.Edit, null)
            }
            IconButton(onClick = { showDeleteSheet = true }) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.onEvent(VaultsEvent.Refresh) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSavingToDownloads = uiState.savingToDownloadsDocIds.contains(doc.id)
                        DocIconWithBadge(
                            fileName = doc.name,
                            isDownloaded = isDownloaded,
                            isSavingToDownloads = isSavingToDownloads,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = doc.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (doc.tag.isNotBlank()) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(VaultDocUtils.getLocalizedTag(doc.tag)) },
                                    leadingIcon = { Icon(Icons.Default.Tag, null, modifier = Modifier.size(14.dp)) }
                                )
                            }

                            if (doc.description.isNotBlank()) {
                                Text(
                                    text = doc.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ActionListItem(
                            headline = Loc.Vaults.docDate,
                            supportingText = dateFormat.format(Date(doc.documentTimestamp)),
                            leadingContent = { Icon(Icons.Default.Event, null) }
                        )
                        ActionListItem(
                            headline = Loc.Vaults.uploadDate,
                            supportingText = uploadDateFormat.format(Date(doc.uploadTimestamp)),
                            leadingContent = { Icon(Icons.Default.CloudUpload, null) }
                        )
                        ActionListItem(
                            headline = if (isDownloaded) Loc.Vaults.downloaded else "Not Downloaded",
                            leadingContent = {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (isDownloaded) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            onClick = {
                                if (isDownloaded) {
                                    showDeleteOfflineSheet = true
                                } else {
                                    showDownloadConfirmSheet = true
                                }
                            }
                        )
                    }
                    val displayFiles = if (doc.fileNames.isNotEmpty()) {
                        doc.fileNames.indices.toList()
                    } else if (doc.driveFileId.isNotEmpty() || doc.driveFileIds.isNotEmpty()) {
                        listOf(0)
                    } else {
                        emptyList()
                    }

                    if (displayFiles.isNotEmpty()) {
                        Text(
                            text = Loc.Vaults.document,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                        )

                        displayFiles.forEach { index ->
                            val fileName = doc.fileNames.getOrNull(index) ?: doc.name
                            val driveUrl = doc.driveUrls.getOrNull(index) ?: doc.effectiveDriveUrl
                            val isFileDownloaded = remember(doc.id, index, uiState.offlineVersion) {
                                VaultDocUtils.isDownloadedLocally(context, doc, index)
                            }
                            val isFileDownloading = uiState.downloadingDocIds.contains(doc.id)

                            ActionListItem(
                                headline = fileName,
                                metadataText = if (isFileDownloading) "Downloading..." else if (isFileDownloaded) Loc.Vaults.downloaded else "",
                                leadingContent = {
                                    DocIconWithBadge(
                                        fileName = fileName,
                                        isDownloaded = isFileDownloaded,
                                        isDownloading = isFileDownloading
                                    )
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (isFileDownloaded) {
                                                    VaultDocUtils.openLocalFile(context, doc, index)
                                                } else {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(driveUrl))
                                                    context.startActivity(intent)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = "View File")
                                        }

                                        IconButton(onClick = { selectedFileIndexForSheet = index }) {
                                            Icon(Icons.Default.MoreVert, null)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shareUrl = if (doc.driveFileIds.size > 1 && doc.folderUrl.isNotEmpty()) doc.folderUrl else doc.effectiveDriveUrl
                    
                    FilledTonalIconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Google Drive")
                            Spacer(Modifier.width(8.dp))
                            Text("Google Drive")
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${doc.name}\n$shareUrl")
                            }
                            context.startActivity(Intent.createChooser(intent, Loc.General.share))
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                            Spacer(Modifier.width(8.dp))
                            Text(Loc.General.share)
                        }
                    }
                }
            }
        }
    }

    if (showEditSheet && uiState.selectedVault != null) {
        EditDocSheet(
            vault = uiState.selectedVault!!,
            doc = doc,
            isUploading = uiState.isUploading,
            onDismiss = { showEditSheet = false },
            onConfirm = { updatedDoc, newFiles, removedIds ->
                viewModel.onEvent(
                    VaultsEvent.UpdateDoc(
                        vault = uiState.selectedVault!!,
                        doc = updatedDoc,
                        newFilesToUpload = newFiles,
                        removedDriveFileIds = removedIds
                    ) { success, errorMsg ->
                        if (success) {
                            showEditSheet = false
                        } else {
                            infoMessage = (errorMsg ?: Loc.Vaults.driveUploadError) to MessageType.ERROR
                        }
                    }
                )
            }
        )
    }

    if (showDeleteSheet) {
        DocDeleteConfirmSheet(
            doc = doc,
            onDismiss = { showDeleteSheet = false },
            onConfirm = {
                viewModel.onEvent(VaultsEvent.DeleteDoc(doc.vaultId, doc.id))
                showDeleteSheet = false
                onBack()
            }
        )
    }

    if (showDownloadConfirmSheet) {
        DownloadConfirmSheet(
            doc = doc,
            onConfirmDownload = {
                viewModel.onEvent(VaultsEvent.DownloadDocForOffline(doc))
                showDownloadConfirmSheet = false
            },
            onDismiss = { showDownloadConfirmSheet = false }
        )
    }

    if (showDeleteOfflineSheet) {
        DeleteLocalFileConfirmSheet(
            doc = doc,
            onDismiss = { showDeleteOfflineSheet = false },
            onConfirmDeleteLocal = {
                viewModel.onEvent(VaultsEvent.DeleteDocOffline(doc))
                showDeleteOfflineSheet = false
            }
        )
    }

    if (selectedFileIndexForSheet != null) {
        val fIndex = selectedFileIndexForSheet!!
        val fName = doc.fileNames.getOrNull(fIndex) ?: doc.name
        val fDriveUrl = doc.driveUrls.getOrNull(fIndex) ?: doc.effectiveDriveUrl
        val isFileCached = VaultDocUtils.isDownloadedLocally(context, doc, fIndex)
        FileActionSheet(
            fileName = fName,
            showDriveButtons = showDriveButtons,
            isDocOwner = isDocOwner,
            canDeleteFile = doc.fileNames.size > 1,
            onOpenFile = {
                if (isFileCached) {
                    VaultDocUtils.openLocalFile(context, doc, fIndex)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fDriveUrl))
                    context.startActivity(intent)
                }
            },
            onSaveToDownloads = {
                val localF = VaultDocUtils.getOfflineLocalFile(context, doc, fIndex)
                scope.launch {
                    val success = if (localF.exists() && localF.length() > 0) {
                        GoogleDriveVaultHelper.saveToPublicDownloads(context, localF.absolutePath, fName)
                    } else {
                        infoMessage = "Saving file to Downloads..." to MessageType.INFO
                        GoogleDriveVaultHelper.saveRemoteDriveFileToPublicDownloads(context, doc, fIndex)
                    }
                    if (success) {
                        infoMessage = Loc.Vaults.savedToDownloads to MessageType.SUCCESS
                    } else {
                        infoMessage = "Failed to save file to Downloads" to MessageType.ERROR
                    }
                }
            },
            onOpenDrive = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fDriveUrl))
                context.startActivity(intent)
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "$fName\n$fDriveUrl")
                }
                context.startActivity(Intent.createChooser(intent, Loc.General.share))
            },
            onDeleteFile = {
                viewModel.onEvent(VaultsEvent.DeleteSpecificFile(doc, fIndex))
            },
            onDismiss = { selectedFileIndexForSheet = null }
        )
    }

    if (showSaveToDownloadsSheet) {
        SaveToDownloadsInfoSheet(
            doc = doc,
            onDismiss = { showSaveToDownloadsSheet = false },
            onConfirmSave = {
                scope.launch {
                    val success = GoogleDriveVaultHelper.saveToPublicDownloads(context, localFile.absolutePath, doc.name)
                    if (success) {
                        infoMessage = Loc.Vaults.savedToDownloads to MessageType.SUCCESS
                    } else {
                        infoMessage = "Failed to save file to Downloads" to MessageType.ERROR
                    }
                }
                showSaveToDownloadsSheet = false
            }
        )
    }

    if (showShareSheet) {
        ShareDocSheet(
            doc = doc,
            onDismiss = { showShareSheet = false }
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
