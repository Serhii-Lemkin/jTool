package com.jtool.android.features.vaults.components.floating_upload_progress

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jtool.android.core.services.FileUploadQueueManager
import com.jtool.android.features.vaults.components.floating_progress_indicator.FloatingProgressIndicator
import com.jtool.android.features.vaults.components.upload_progress_sheet.UploadProgressSheet

@Composable
fun FloatingUploadProgressIndicator(
    modifier: Modifier = Modifier,
    forceShowSheet: Boolean = false,
    onSheetDismissed: () -> Unit = {}
) {
    val uploadState by FileUploadQueueManager.uploadState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(forceShowSheet) {
        if (forceShowSheet) showSheet = true
    }

    if (showSheet) {
        UploadProgressSheet(
            onDismiss = {
                showSheet = false
                onSheetDismissed()
            }
        )
    }

    val isFailed = remember(uploadState.tasks) { uploadState.tasks.any { it.isFailed } }

    FloatingProgressIndicator(
        icon = Icons.Default.CloudUpload,
        color = Color(0xFF2563EB),
        progressPercent = uploadState.overallProgress,
        isProcessing = uploadState.isUploading,
        isFailed = isFailed,
        onClick = { showSheet = true },
        modifier = modifier
    )
}
