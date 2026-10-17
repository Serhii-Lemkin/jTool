package com.jtool.android.features.vaults.components.upload_progress_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.services.FileUploadQueueManager
import com.jtool.android.core.services.UploadFileStatus
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.components.cancel_upload_confirm_sheet.CancelUploadConfirmSheet
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun UploadProgressSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uploadState by FileUploadQueueManager.uploadState.collectAsState()

    var pendingCancelFile by remember { mutableStateOf<Triple<String, String, String>?>(null) } // taskId, fileId, fileName
    var pendingCancelTask by remember { mutableStateOf<Pair<String, String>?>(null) } // taskId, docName

    if (pendingCancelFile != null) {
        val (taskId, fileId, fileName) = pendingCancelFile!!
        CancelUploadConfirmSheet(
            title = "Cancel File Upload?",
            message = "Are you sure you want to cancel uploading '$fileName'?",
            onConfirmCancel = {
                FileUploadQueueManager.cancelFileInTask(taskId, fileId)
                pendingCancelFile = null
            },
            onDismiss = { pendingCancelFile = null }
        )
    }

    if (pendingCancelTask != null) {
        val (taskId, docName) = pendingCancelTask!!
        CancelUploadConfirmSheet(
            title = "Cancel Record Upload?",
            message = "Are you sure you want to cancel uploading '$docName'? Any uploaded files for this record will be removed from Google Drive.",
            onConfirmCancel = {
                FileUploadQueueManager.cancelTask(taskId)
                pendingCancelTask = null
            },
            onDismiss = { pendingCancelTask = null }
        )
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.General.uploadProgress
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uploadState.tasks.isEmpty()) {
                Text(
                    text = "No active uploads",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uploadState.tasks.forEach { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isAllCompleted = task.files.all { it.status == UploadFileStatus.COMPLETED }
                                Text(
                                    text = task.docName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isAllCompleted) Loc.General.uploadSuccess else "${task.files.size} file(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            pendingCancelTask = task.id to task.docName
                                        },
                                        modifier = Modifier.size(28.dp).padding(start = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Cancel All",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            task.files.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "${file.fileName}${file.extension}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = VaultDocUtils.formatFileSize(file.fileSize),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    when (file.status) {
                                        UploadFileStatus.COMPLETED -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        UploadFileStatus.FAILED -> {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = "Failed",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        else -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${file.progressPercent}%",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                IconButton(
                                                    onClick = {
                                                        pendingCancelFile = Triple(task.id, file.id, "${file.fileName}${file.extension}")
                                                    },
                                                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Cancel File",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (task.isFailed) {
                                Text(
                                    text = task.errorMessage ?: "Upload failed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(
                                    onClick = {
                                        FileUploadQueueManager.removeTask(task.id)
                                        // Re-enqueue
                                        val attachedFiles = task.files.map { f ->
                                            com.jtool.android.features.vaults.AttachedFile(
                                                uri = f.uri,
                                                fileName = f.fileName,
                                                extension = f.extension
                                            )
                                        }
                                        FileUploadQueueManager.enqueue(
                                            context = context,
                                            vault = task.vault,
                                            files = attachedFiles,
                                            docName = task.docName,
                                            tag = task.tag,
                                            docDate = task.docDate,
                                            description = task.description,
                                            saveOffline = task.saveOffline
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retry Upload", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
