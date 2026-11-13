package com.jtool.android.features.vaults.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.services.DownloadFileStatus
import com.jtool.android.core.services.DownloadState
import com.jtool.android.core.services.UploadFileStatus
import com.jtool.android.core.services.UploadState
import com.jtool.android.core.utils.getPreferredSimpleDateFormat
import com.jtool.android.features.vaults.VaultsUiState
import com.jtool.android.features.vaults.common.DocIconWithBadge
import com.jtool.android.features.vaults.common.VaultDocUtils
import java.util.Date

@Composable
fun VaultDocListItem(
    doc: VaultDocument,
    uiState: VaultsUiState,
    downloadState: DownloadState,
    uploadState: UploadState,
    onOpenFilesSheet: () -> Unit,
    onUpdatingBlocked: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenActionSheet: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember(context) { getPreferredSimpleDateFormat(context) }
    val dateStr = dateFormat.format(Date(doc.documentTimestamp))
    val isDownloaded = remember(doc.id, uiState.offlineVersion, downloadState) {
        VaultDocUtils.isDocumentDownloadedLocally(context, doc)
    }
    val isDownloading = remember(doc.id, uiState.downloadingDocIds, downloadState) {
        uiState.downloadingDocIds.contains(doc.id) ||
            downloadState.tasks.any {
                !it.isFailed && it.doc.id == doc.id && it.files.any { f ->
                    f.status == DownloadFileStatus.PENDING || f.status == DownloadFileStatus.DOWNLOADING
                }
            }
    }
    val isDeleting = uiState.deletingDocIds.contains(doc.id)
    val firstFileName = doc.fileNames.firstOrNull() ?: doc.name
    val fileCount = if (doc.fileNames.isNotEmpty()) doc.fileNames.size else if (doc.driveFileIds.isNotEmpty()) doc.driveFileIds.size else 1
    val tagText = if (doc.tag.isNotBlank()) "${VaultDocUtils.getLocalizedTag(doc.tag)} • " else ""

    val isUpdating = remember(doc.id, uiState.updatingDocIds, uploadState) {
        uiState.updatingDocIds.contains(doc.id) ||
            uploadState.tasks.any {
                !it.isFailed && it.existingDocId == doc.id && it.files.any { f ->
                    f.status != UploadFileStatus.COMPLETED
                }
            }
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
                onUpdatingBlocked()
            } else if (!isDeleting) {
                onOpenFilesSheet()
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
                        overflow = TextOverflow.Ellipsis
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
                            onUpdatingBlocked()
                        } else {
                            onOpenDetails()
                        }
                    }
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Doc Details")
                }

                if (!isDeleting) {
                    IconButton(
                        onClick = {
                            if (isUpdating) {
                                onUpdatingBlocked()
                            } else {
                                onOpenActionSheet(isDownloaded)
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
