package com.jtool.android.features.vaults.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.services.UploadFileStatus
import com.jtool.android.core.services.UploadState
import com.jtool.android.core.utils.getPreferredSimpleDateFormat
import com.jtool.android.features.vaults.VaultsUiState
import com.jtool.android.features.vaults.common.DocIconWithBadge
import com.jtool.android.features.vaults.common.VaultDocUtils
import java.util.Date

@Composable
fun GroupMatchedDocListItem(
    doc: VaultDocument,
    vault: Vault?,
    uiState: VaultsUiState,
    uploadState: UploadState,
    onUpdatingBlocked: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember(context) { getPreferredSimpleDateFormat(context) }
    val dateStr = dateFormat.format(Date(doc.documentTimestamp))
    val isDownloaded = VaultDocUtils.isDocumentDownloadedLocally(context, doc)
    val isUpdating = remember(doc.id, uiState.updatingDocIds, uploadState) {
        uiState.updatingDocIds.contains(doc.id) ||
            uploadState.tasks.any { !it.isFailed && it.existingDocId == doc.id && it.files.any { f -> f.status != UploadFileStatus.COMPLETED } }
    }
    val firstFileName = doc.fileNames.firstOrNull() ?: doc.name
    val fileCount = if (doc.fileNames.isNotEmpty()) doc.fileNames.size else if (doc.driveFileIds.isNotEmpty()) doc.driveFileIds.size else 1

    val metadataStr = when {
        isUpdating -> Loc.s("vaults_updatingBg", "Updating in background...")
        isDownloaded -> Loc.Vaults.downloaded
        else -> null
    }

    ActionListItem(
        onClick = {
            if (isUpdating) {
                onUpdatingBlocked()
            } else if (vault != null) {
                onOpenDetails()
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
                    if (vault != null) {
                        Text(
                            text = "${vault.name} • ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (doc.tag.isNotBlank()) {
                        Text(
                            text = "${VaultDocUtils.getLocalizedTag(doc.tag)} • ",
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
                if (metadataStr != null) {
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
                isDownloading = isUpdating
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    if (isUpdating) {
                        onUpdatingBlocked()
                    } else if (vault != null) {
                        onOpenDetails()
                    }
                }
            ) {
                Icon(Icons.Default.Info, contentDescription = "Doc Details")
            }
        }
    )
}
