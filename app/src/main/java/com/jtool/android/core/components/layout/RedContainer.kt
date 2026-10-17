package com.jtool.android.core.components.layout

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.BiasAlignment
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jtool.android.core.services.FileDownloadQueueManager
import com.jtool.android.core.services.FileExportQueueManager
import com.jtool.android.core.services.FileUploadQueueManager
import com.jtool.android.core.services.NotificationQueueManager
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.vaults.components.floating_download_progress.FloatingDownloadProgressIndicator
import com.jtool.android.features.vaults.components.floating_export_progress.FloatingExportProgressIndicator
import com.jtool.android.features.vaults.components.floating_upload_progress.FloatingUploadProgressIndicator

@Composable
fun BoxScope.RedContainer(
    screenTitle: String,
    hasNotifications: Boolean = false,
    notificationCount: Int = 0,
    onNotificationsClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val notifState by NotificationQueueManager.notificationState.collectAsState()
    val uploadState by FileUploadQueueManager.uploadState.collectAsState()
    val downloadState by FileDownloadQueueManager.downloadState.collectAsState()
    val exportState by FileExportQueueManager.exportState.collectAsState()

    val hasUploadActivity by remember {
        derivedStateOf { uploadState.isUploading || uploadState.tasks.any { it.isFailed } }
    }
    val hasDownloadActivity by remember {
        derivedStateOf { downloadState.isDownloading || downloadState.tasks.any { it.isFailed } }
    }
    val hasExportActivity by remember {
        derivedStateOf { exportState.isExporting || exportState.tasks.any { it.isFailed } }
    }

    val totalNotifCount = if (notificationCount > 0) notificationCount else notifState.notificationCount
    val handleNotifClick = onNotificationsClick ?: notifState.onNotificationsClick
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var containerSide by remember { mutableStateOf(settingsManager.getFloatingContainerSide()) }
    val isLeftSide = containerSide == "LEFT"
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var isCollapsed by remember { mutableStateOf(settingsManager.isFloatingContainerCollapsed()) }
    val setCollapsedState = { collapsed: Boolean ->
        isCollapsed = collapsed
        settingsManager.setFloatingContainerCollapsed(collapsed)
    }

    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    val targetBias = if (isLeftSide != isRtl) -1f else 1f
    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SideSwitchBiasAnimation"
    )
    val physicalAlignment = BiasAlignment(horizontalBias = animatedBias, verticalBias = 1f)

    AnimatedContent(
        targetState = isCollapsed,
        transitionSpec = {
            if (targetState) {
                fadeIn() + slideInHorizontally { if (isLeftSide != isRtl) -it else it } togetherWith fadeOut() + slideOutHorizontally { if (isLeftSide != isRtl) -it else it }
            } else {
                fadeIn() + slideInHorizontally { if (isLeftSide != isRtl) -it else it } togetherWith fadeOut() + slideOutHorizontally { if (isLeftSide != isRtl) -it else it }
            }
        },
        modifier = Modifier
            .align(physicalAlignment)
            .navigationBarsPadding()
            .padding(
                bottom = 16.dp,
                start = if (isLeftSide != isRtl) (if (isCollapsed) 0.dp else 16.dp) else 0.dp,
                end = if (isLeftSide == isRtl) (if (isCollapsed) 0.dp else 16.dp) else 0.dp
            ),
        label = "RedContainerCollapse"
    ) { collapsed ->
        if (collapsed) {
            Column(
                modifier = Modifier
                    .clip(if (isLeftSide != isRtl) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .clickable { setCollapsedState(false) }
                    .pointerInput(containerSide, isRtl) {
                        var handleDragSum = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { handleDragSum = 0f },
                            onDragEnd = {
                                if (isLeftSide != isRtl) {
                                    if (handleDragSum > 20f) setCollapsedState(false)
                                } else {
                                    if (handleDragSum < -20f) setCollapsedState(false)
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                handleDragSum += dragAmount
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasNotifications || totalNotifCount > 0) {
                    Surface(
                        shape = if (isLeftSide != isRtl) RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp) else RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(width = 18.dp, height = 28.dp)
                    ) {}
                }
                if (hasUploadActivity) {
                    Surface(
                        shape = if (isLeftSide != isRtl) RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp) else RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(width = 18.dp, height = 28.dp)
                    ) {}
                }
                if (hasDownloadActivity) {
                    Surface(
                        shape = if (isLeftSide != isRtl) RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp) else RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        color = Color(0xFF16A34A),
                        modifier = Modifier.size(width = 18.dp, height = 28.dp)
                    ) {}
                }
                if (hasExportActivity) {
                    Surface(
                        shape = if (isLeftSide != isRtl) RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp) else RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        color = Color(0xFFEA580C),
                        modifier = Modifier.size(width = 18.dp, height = 28.dp)
                    ) {}
                }
                Surface(
                    shape = if (isLeftSide != isRtl) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(width = 24.dp, height = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isLeftSide != isRtl) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .pointerInput(containerSide, isRtl, screenWidthPx) {
                        var dragSum = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragSum = 0f },
                            onDragEnd = {
                                val halfScreen = screenWidthPx / 2f
                                if (isLeftSide != isRtl) {
                                    if (dragSum < -30f) {
                                        setCollapsedState(true)
                                    } else if (dragSum > halfScreen) {
                                        containerSide = "RIGHT"
                                        settingsManager.setFloatingContainerSide("RIGHT")
                                    }
                                } else {
                                    if (dragSum > 30f) {
                                        setCollapsedState(true)
                                    } else if (dragSum < -halfScreen) {
                                        containerSide = "LEFT"
                                        settingsManager.setFloatingContainerSide("LEFT")
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragSum += dragAmount
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Notification Bell Icon Button
                AnimatedVisibility(
                    visible = hasNotifications || totalNotifCount > 0,
                    enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn() + slideInVertically { it / 2 },
                    exit = scaleOut() + fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { handleNotifClick?.invoke() }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // 2. Upload Progress FAB
                FloatingUploadProgressIndicator()

                // 3. Download Progress FAB
                FloatingDownloadProgressIndicator()

                // 4. Export Progress FAB
                FloatingExportProgressIndicator()

                // 5. Child Screen's Floating Action Buttons (e.g. Plus FAB)
                content()
            }
        }
    }
}
