package com.jtool.android.features.calendar.settings.components.sync_info_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun SyncInfoSheet(
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Calendar.syncInfo,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoSection(Loc.Calendar.enableSync, Loc.Calendar.syncInfoEnable)
            InfoSection(Loc.Calendar.pullEvents, Loc.Calendar.syncInfoPull)
            InfoSection(Loc.Calendar.pushEvents, Loc.Calendar.syncInfoPush)
            InfoSection(Loc.Calendar.shared, Loc.Calendar.syncInfoPartial)
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(Loc.General.gotIt)
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, description: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
