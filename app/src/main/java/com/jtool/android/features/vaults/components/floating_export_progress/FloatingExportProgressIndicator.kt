package com.jtool.android.features.vaults.components.floating_export_progress

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jtool.android.core.services.FileExportQueueManager
import com.jtool.android.features.vaults.components.export_progress_sheet.ExportProgressSheet
import com.jtool.android.features.vaults.components.floating_progress_indicator.FloatingProgressIndicator

@Composable
fun FloatingExportProgressIndicator(
    modifier: Modifier = Modifier
) {
    val exportState by FileExportQueueManager.exportState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        ExportProgressSheet(onDismiss = { showSheet = false })
    }

    val isFailed = remember(exportState.tasks) { exportState.tasks.any { it.isFailed } }

    FloatingProgressIndicator(
        icon = Icons.Default.Folder,
        color = Color(0xFFEA580C),
        progressPercent = exportState.overallProgress,
        isProcessing = exportState.isExporting,
        isFailed = isFailed,
        onClick = { showSheet = true },
        modifier = modifier
    )
}
