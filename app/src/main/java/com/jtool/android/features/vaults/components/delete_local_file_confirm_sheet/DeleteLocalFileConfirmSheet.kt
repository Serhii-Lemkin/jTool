package com.jtool.android.features.vaults.components.delete_local_file_confirm_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
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
fun DeleteLocalFileConfirmSheet(
    doc: VaultDocument,
    onDismiss: () -> Unit,
    onConfirmDeleteLocal: () -> Unit
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
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(52.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Remove Offline Copy?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "This file will be removed from your phone's offline storage. It will remain safely saved in Google Drive and can be re-downloaded anytime.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onConfirmDeleteLocal()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Remove Offline", fontWeight = FontWeight.Bold)
            }
        }
    }
}
