package com.jtool.android.features.vaults.components.floating_progress_indicator

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun FloatingProgressIndicator(
    icon: ImageVector,
    color: Color,
    progressPercent: Int,
    isProcessing: Boolean,
    isFailed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isProcessing || isFailed,
        enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn() + slideInVertically { it / 2 },
        exit = scaleOut() + fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            contentColor = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable { onClick() }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier.size(38.dp),
                        strokeWidth = 3.dp,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
            }
        }
    }
}
