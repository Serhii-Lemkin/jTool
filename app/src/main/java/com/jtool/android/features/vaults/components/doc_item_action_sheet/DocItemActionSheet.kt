package com.jtool.android.features.vaults.components.doc_item_action_sheet

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun DocItemActionSheet(
    doc: VaultDocument,
    isDownloaded: Boolean,
    isDocOwner: Boolean,
    showDriveButtons: Boolean = false,
    onOpenDrive: () -> Unit,
    onOpenFolder: () -> Unit,
    onDownloadOffline: () -> Unit,
    onRemoveOffline: () -> Unit,
    onSaveToDownloads: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = doc.name
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Open Connected Folder in Drive (Only if showDriveButtons == true)
            if (showDriveButtons) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (doc.folderUrl.isNotEmpty()) onOpenFolder() else onOpenDrive()
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(VaultsLoc.openDriveFolder, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Open connected folder in Google Drive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }

            // 2. Offline Cache option
            if (isDownloaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onRemoveOffline()
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Remove Offline Copy", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Remove local cached copy from device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDownloadOffline()
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(Loc.Vaults.downloadForOffline, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Download and cache files on device for offline view", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // 3. Save to Downloads (Always available!)
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
                    Text("Save file to phone's public Downloads folder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Share document link and details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 4. Edit & Delete
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEdit()
                        onDismiss()
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(Loc.General.edit, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Edit document name, tag, date, and files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (isDocOwner) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDelete()
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(Loc.General.delete, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Permanently delete document record and files from Drive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
