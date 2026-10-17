package com.jtool.android.features.vaults.components.file_action_sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun FileActionSheet(
    fileName: String,
    showDriveButtons: Boolean = false,
    isDocOwner: Boolean = false,
    canDeleteFile: Boolean = false,
    onOpenFile: () -> Unit,
    onSaveToDownloads: () -> Unit,
    onOpenDrive: () -> Unit,
    onShare: () -> Unit,
    onDeleteFile: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = fileName
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Open File
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenFile()
                        onDismiss()
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Open File", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("View file content", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // 2. Save to Downloads
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSaveToDownloads()
                        onDismiss()
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(Loc.Vaults.saveToDownloads, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Save file to device Downloads folder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 3. Open in Drive (if showDriveButtons)
            if (showDriveButtons) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenDrive()
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Google Drive", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Open file link in Google Drive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // 4. Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShare()
                        onDismiss()
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(Loc.General.share, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Share file link and details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 5. Delete File (if isDocOwner && canDeleteFile)
            if (isDocOwner && canDeleteFile) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDeleteFile()
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Delete File", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Remove this file from document record", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
