package com.jtool.android.features.settings.groups.components.group_created_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun GroupCreatedSheet(
    groupId: String,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Settings.congratulations
    ) {
        Icon(
            Icons.Default.CheckCircle,
            null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = Loc.Settings.groupCreatedSuccess,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(32.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val fontSize = if (groupId.length > 25) 12.sp else if (groupId.length > 20) 16.sp else 20.sp
                Text(
                    text = Loc.Settings.groupIdHeader,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = groupId,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { onCopy(groupId) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.ContentCopy, null)
            Spacer(Modifier.width(8.dp))
            Text(Loc.Settings.copyId)
        }
    }
}
