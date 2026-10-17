package com.jtool.android.features.vaults.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import com.jtool.android.features.vaults.components.clear_cache_confirm_sheet.ClearCacheConfirmSheet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.VaultsEvent
import com.jtool.android.features.vaults.VaultsViewModel
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType

@Composable
fun VaultSettingsScreen(
    dataManager: DataManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    viewModel: VaultsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var isWarningEnabled by remember { mutableStateOf(settingsManager.isLargeFileWarningEnabled()) }
    var maxSizeMbText by remember { mutableStateOf(settingsManager.getMaxFileSizeWithoutWarningMb().toString()) }
    var showDriveButtons by remember { mutableStateOf(settingsManager.isShowDriveButtonsEnabled()) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }
    var showClearCacheSheet by remember { mutableStateOf(false) }

    BaseScreen(
        title = "Vault Settings",
        isLoading = uiState.isLoading,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(VaultsEvent.Refresh) }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Storage & Warning Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // 1. Warn on Large Files Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = !isWarningEnabled
                        settingsManager.setLargeFileWarningEnabled(next)
                        isWarningEnabled = next
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Warn on Large Files", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Show warning before uploading or caching large files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = isWarningEnabled,
                    onCheckedChange = { checked ->
                        settingsManager.setLargeFileWarningEnabled(checked)
                        isWarningEnabled = checked
                    }
                )
            }

            // 2. Max File Size Input (when warning enabled)
            if (isWarningEnabled) {
                OutlinedTextField(
                    value = maxSizeMbText,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        maxSizeMbText = clean
                        val mb = clean.toIntOrNull() ?: 100
                        settingsManager.setMaxFileSizeWithoutWarningMb(mb)
                    },
                    label = { Text("Max File Size Without Warning (MB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // 3. Show 'Open in Drive' Buttons Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = !showDriveButtons
                        settingsManager.setShowDriveButtonsEnabled(next)
                        showDriveButtons = next
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Show 'Open in Drive' buttons", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Display direct Google Drive folder and file links on Vault items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = showDriveButtons,
                    onCheckedChange = { checked ->
                        settingsManager.setShowDriveButtonsEnabled(checked)
                        showDriveButtons = checked
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            Text(
                text = "Maintenance & Sync",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // 3. Reapply Permissions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onEvent(VaultsEvent.ReapplyPermissions)
                        infoMessage = VaultsLoc.reapplyPermissionsSuccess to MessageType.SUCCESS
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
                    Text(VaultsLoc.reapplyPermissions, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Re-sync Google Drive folder permissions with group members", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // 4. Validate Cached Data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.onEvent(VaultsEvent.ValidateCachedData {
                            infoMessage = VaultsLoc.validateVaultsSuccess to MessageType.SUCCESS
                        })
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
                    Text(VaultsLoc.validateVaults, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Delete stale local files and validate storage cache", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // 5. Clear Local Vault Cache
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showClearCacheSheet = true
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Clear Local Vault Cache", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Delete all offline vault files from device and reset cache status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showClearCacheSheet) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ClearCacheConfirmSheet(
            onConfirmClear = {
                try {
                    val vaultsDir = java.io.File(context.filesDir, "vaults")
                    if (vaultsDir.exists()) vaultsDir.deleteRecursively()
                    vaultsDir.mkdirs()
                    viewModel.onEvent(VaultsEvent.ValidateCachedData {
                        infoMessage = "Local vault cache cleared!" to MessageType.SUCCESS
                    })
                } catch (e: Exception) {
                    infoMessage = "Error clearing cache: ${e.message}" to MessageType.ERROR
                }
            },
            onDismiss = { showClearCacheSheet = false }
        )
    }

    if (infoMessage != null) {
        MessageSheet(
            message = infoMessage!!.first,
            type = infoMessage!!.second,
            onDismiss = { infoMessage = null }
        )
    }
}
