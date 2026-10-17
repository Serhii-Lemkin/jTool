package com.jtool.android.features.vaults.components.vault_settings_menu_sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun VaultSettingsMenuSheet(
    isVaultOwner: Boolean,
    onCheckPermissions: () -> Unit,
    onReapplyPermissions: () -> Unit,
    onValidateCachedData: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = "Vault Options"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Check Permissions (Available to all users)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onCheckPermissions()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = Loc.Vaults.checkPermissions,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Check your Google Drive permission status for this Vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Reapply Permissions & Validate Cached Data (Vault Owner / Admin only)
            if (isVaultOwner) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onReapplyPermissions()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = VaultsLoc.reapplyPermissions,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sync Google Drive folder permissions for group",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onValidateCachedData()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = VaultsLoc.validateVaults,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clean up stale local files and sync cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
