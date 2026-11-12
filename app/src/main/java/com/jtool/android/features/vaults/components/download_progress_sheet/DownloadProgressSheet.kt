package com.jtool.android.features.vaults.components.download_progress_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.services.DownloadFileStatus
import com.jtool.android.core.services.FileDownloadQueueManager
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun DownloadProgressSheet(
    onDismiss: () -> Unit
) {
    val downloadState by FileDownloadQueueManager.downloadState.collectAsState()

    GenericSheet(
        onDismiss = {
            FileDownloadQueueManager.clearFinishedTasks()
            onDismiss()
        },
        title = "Caching Offline Progress"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (downloadState.tasks.isEmpty()) {
                Text(
                    text = "No active downloads",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                downloadState.tasks.forEach { task ->
                    val isAllCompleted = task.files.all { it.status == DownloadFileStatus.COMPLETED }
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
                                Text(
                                    text = task.doc.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAllCompleted) VaultsLoc.allFilesDownloaded else "${task.files.size} file(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
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
                                    Text(
                                        text = file.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )

                                    when (file.status) {
                                        DownloadFileStatus.COMPLETED -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        DownloadFileStatus.FAILED -> {
                                            val context = LocalContext.current
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Error,
                                                    contentDescription = "Failed",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(24.dp).padding(end = 4.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        FileDownloadQueueManager.retryTask(context, task.id)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Retry",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
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
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    }
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
