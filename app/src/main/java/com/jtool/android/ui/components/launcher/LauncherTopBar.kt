package com.jtool.android.ui.components.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.services.FileDownloadQueueManager
import com.jtool.android.core.services.FileExportQueueManager
import com.jtool.android.core.services.FileUploadQueueManager
import com.jtool.android.features.vaults.components.download_progress_sheet.DownloadProgressSheet
import com.jtool.android.features.vaults.components.export_progress_sheet.ExportProgressSheet
import com.jtool.android.features.vaults.components.upload_progress_sheet.UploadProgressSheet

@Composable
fun LauncherTopBar(
    onSupportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isAdmin: Boolean,
    currentUser: FirebaseUser?,
    onAdminClick: () -> Unit,
    notificationCount: Int,
    onNotificationsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSupportClick) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = Loc.Launcher.supportHub,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = Loc.Launcher.settings,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (currentUser != null && isAdmin) {
                IconButton(onClick = onAdminClick) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = Loc.Launcher.adminPanel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val uploadState by FileUploadQueueManager.uploadState.collectAsState()
        val downloadState by FileDownloadQueueManager.downloadState.collectAsState()
        val exportState by FileExportQueueManager.exportState.collectAsState()

        var showUploadSheet by remember { mutableStateOf(false) }
        var showDownloadSheet by remember { mutableStateOf(false) }
        var showExportSheet by remember { mutableStateOf(false) }

        if (showUploadSheet) UploadProgressSheet(onDismiss = { showUploadSheet = false })
        if (showDownloadSheet) DownloadProgressSheet(onDismiss = { showDownloadSheet = false })
        if (showExportSheet) ExportProgressSheet(onDismiss = { showExportSheet = false })

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uploadState.isUploading || uploadState.tasks.any { it.isFailed }) {
                IconButton(
                    onClick = { showUploadSheet = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Uploads",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (downloadState.isDownloading || downloadState.tasks.any { it.isFailed }) {
                IconButton(
                    onClick = { showDownloadSheet = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Downloads",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (exportState.isExporting || exportState.tasks.any { it.isFailed }) {
                IconButton(
                    onClick = { showExportSheet = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFEA580C).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Exports",
                        tint = Color(0xFFEA580C)
                    )
                }
            }

            val hasNotifications = notificationCount > 0
            BadgedBox(
                badge = {
                    if (hasNotifications) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (hasNotifications) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = Loc.General.notifications,
                        tint = if (hasNotifications) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
