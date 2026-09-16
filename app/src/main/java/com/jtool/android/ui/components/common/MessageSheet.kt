package com.jtool.android.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc

enum class MessageType {
    INFO, SUCCESS, ERROR
}

@Composable
fun MessageSheet(
    message: String,
    title: String? = null,
    type: MessageType = MessageType.INFO,
    onDismiss: () -> Unit
) {
    val icon: ImageVector = when (type) {
        MessageType.INFO -> Icons.Default.Info
        MessageType.SUCCESS -> Icons.Default.CheckCircle
        MessageType.ERROR -> Icons.Default.Error
    }

    val color: Color = when (type) {
        MessageType.INFO -> MaterialTheme.colorScheme.primary
        MessageType.SUCCESS -> Color(0xFF4CAF50)
        MessageType.ERROR -> MaterialTheme.colorScheme.error
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = title,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
        }
    }
}
