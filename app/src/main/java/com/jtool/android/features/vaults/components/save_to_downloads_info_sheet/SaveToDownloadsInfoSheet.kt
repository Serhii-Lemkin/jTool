package com.jtool.android.features.vaults.components.save_to_downloads_info_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun SaveToDownloadsInfoSheet(
    doc: VaultDocument,
    onDismiss: () -> Unit,
    onConfirmSave: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = Loc.Vaults.saveToDownloads,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "This will save a copy of \"${doc.name}\" to your phone's public Downloads folder (/storage/emulated/0/Download). You can access and open it anytime using any file manager app.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(Loc.General.cancel)
                }

                Button(
                    onClick = onConfirmSave,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(Loc.General.save, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
