package com.jtool.android.features.vaults.components.doc_files_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.models.vaults.enums.FileOpenErrorReasonEnum
import com.jtool.android.core.services.FileDownloadQueueManager
import com.jtool.android.features.vaults.common.DocIconWithBadge
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.components.file_open_error_sheet.FileOpenErrorSheet
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun DocFilesSheet(
    doc: VaultDocument,
    offlineVersion: Long = 0L,
    onOpenError: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var openErrorReason by remember { mutableStateOf<FileOpenErrorReasonEnum?>(null) }
    val filesList = remember(doc, offlineVersion) {
        if (doc.fileNames.isNotEmpty()) {
            doc.fileNames.indices.map { i ->
                val name = doc.fileNames[i]
                val driveUrl = doc.getFileDriveUrl(i)
                val isDownloaded = VaultDocUtils.isDownloadedLocally(context, doc, i)
                Triple(name, driveUrl, isDownloaded)
            }
        } else {
            val name = doc.name
            val driveUrl = doc.effectiveDriveUrl
            val isDownloaded = VaultDocUtils.isDownloadedLocally(context, doc, 0)
            listOf(Triple(name, driveUrl, isDownloaded))
        }
    }

    val downloadState by FileDownloadQueueManager.downloadState.collectAsState()

    GenericSheet(
        onDismiss = onDismiss,
        title = doc.name
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filesList.forEachIndexed { index, (fileName, driveUrl, _) ->
                val isDownloaded = remember(doc.id, index, offlineVersion, downloadState) {
                    VaultDocUtils.isDownloadedLocally(context, doc, index)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocIconWithBadge(
                        fileName = fileName,
                        isDownloaded = isDownloaded,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    if (isDownloaded) {
                        IconButton(
                            onClick = {
                                val reason = VaultDocUtils.openLocalFileWithReason(context, doc, index)
                                if (reason != null) {
                                    openErrorReason = reason
                                    onOpenError()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "View File",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val reason = VaultDocUtils.openExternalUrlWithReason(context, driveUrl)
                            if (reason != null) {
                                openErrorReason = reason
                                onOpenError()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Drive",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (openErrorReason != null) {
        FileOpenErrorSheet(
            reason = openErrorReason!!,
            onDismiss = { openErrorReason = null }
        )
    }
}
