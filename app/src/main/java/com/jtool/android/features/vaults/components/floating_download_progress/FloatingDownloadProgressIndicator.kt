package com.jtool.android.features.vaults.components.floating_download_progress

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jtool.android.core.services.FileDownloadQueueManager
import com.jtool.android.features.vaults.components.download_progress_sheet.DownloadProgressSheet
import com.jtool.android.features.vaults.components.floating_progress_indicator.FloatingProgressIndicator

@Composable
fun FloatingDownloadProgressIndicator(
    modifier: Modifier = Modifier
) {
    val downloadState by FileDownloadQueueManager.downloadState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        DownloadProgressSheet(onDismiss = { showSheet = false })
    }

    val isFailed = remember(downloadState.tasks) { downloadState.tasks.any { it.isFailed } }

    FloatingProgressIndicator(
        icon = Icons.Default.CloudDownload,
        color = Color(0xFF16A34A),
        progressPercent = downloadState.overallProgress,
        isProcessing = downloadState.isDownloading,
        isFailed = isFailed,
        onClick = { showSheet = true },
        modifier = modifier
    )
}
