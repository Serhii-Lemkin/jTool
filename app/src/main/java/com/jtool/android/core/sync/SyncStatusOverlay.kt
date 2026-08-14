package com.jtool.android.core.sync

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SyncStatusOverlay() {
    val status = SyncStatusManager.status
    if (status == SyncStatus.IDLE) return

    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .statusBarsPadding()
            .padding(16.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopEnd
    ) {
        Card(
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                when (status) {
                    SyncStatus.SYNCING -> {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Syncing",
                            modifier = Modifier.rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    SyncStatus.SUCCESS -> {
                        // Success state icon removed as per user request
                    }
                    SyncStatus.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
