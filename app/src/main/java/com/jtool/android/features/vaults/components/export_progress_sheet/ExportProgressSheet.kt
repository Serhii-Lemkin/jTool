package com.jtool.android.features.vaults.components.export_progress_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.jtool.android.core.services.ExportFileStatus
import com.jtool.android.core.services.FileExportQueueManager
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun ExportProgressSheet(
    onDismiss: () -> Unit
) {
    val exportState by FileExportQueueManager.exportState.collectAsState()
    val context = LocalContext.current

    val handleDismiss = {
        FileExportQueueManager.clearCompletedTasks()
        onDismiss()
    }

    GenericSheet(
        onDismiss = handleDismiss,
        title = "Export to Downloads Progress"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (exportState.tasks.isEmpty()) {
                Text(
                    text = "No active exports",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                exportState.tasks.forEach { task ->
                    val isAllCompleted = task.files.all { it.status == ExportFileStatus.COMPLETED }
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
                                    text = if (isAllCompleted) "Saved to Downloads" else "${task.files.size} file(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFEA580C),
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
                                        ExportFileStatus.COMPLETED -> {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        ExportFileStatus.FAILED -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Error,
                                                    contentDescription = "Failed",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(24.dp).padding(end = 4.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        FileExportQueueManager.retryTask(context, task.id)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Retry",
                                                        tint = Color(0xFFEA580C),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFFEA580C)
                                            )
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
