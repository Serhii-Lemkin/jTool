package com.jtool.android.features.vaults

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.vaults.components.floating_upload_progress.FloatingUploadProgressIndicator
import com.jtool.android.features.vaults.components.vaults_info_sheet.VaultsInfoSheet
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.components.create_vault_sheet.CreateVaultSheet
import com.jtool.android.features.vaults.components.vault_delete_confirm_sheet.VaultDeleteConfirmSheet
import com.jtool.android.features.vaults.components.vault_delete_warning_sheet.VaultDeleteWarningSheet
import com.jtool.android.features.vaults.components.vault_item_action_sheet.VaultItemActionSheet
import com.jtool.android.features.vaults.components.vaults_list_settings_sheet.VaultsListSettingsSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    dataManager: DataManager,
    settingsManager: SettingsManager,
    authManager: AuthManager,
    isAdmin: Boolean,
    currentGroupId: String?,
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: VaultsViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = remember { settingsManager.getAppUserId() }
    val authUid = remember { authManager.getCurrentUser()?.uid ?: "" }
    val showDriveButtons = remember { settingsManager.isShowDriveButtonsEnabled() }
    val isOnline = remember { VaultDocUtils.isNetworkAvailable(context) }

    var showCreateSheet by remember { mutableStateOf(false) }
    var editingVault by remember { mutableStateOf<Vault?>(null) }
    var vaultForActionSheet by remember { mutableStateOf<Vault?>(null) }
    
    var warningDeleteVault by remember { mutableStateOf<Vault?>(null) }
    var confirmDeleteVault by remember { mutableStateOf<Vault?>(null) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }
    var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }

    val pagedVaults = remember(uiState.filteredVaults, uiState.pagingLimit) {
        uiState.filteredVaults.take(uiState.pagingLimit)
    }

    LaunchedEffect(uiState.searchQuery) { draftSearchQuery = uiState.searchQuery }
    LaunchedEffect(currentGroupId) { viewModel.setGroupId(currentGroupId) }

    var showVaultsInfoSheet by remember { mutableStateOf(false) }
    var showVaultsListSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = authManager.getCurrentUser()?.uid
        if (uid != null) {
            val seenInfo = settingsManager.getSeenVaultsInfo()
            if (!seenInfo) {
                showVaultsInfoSheet = true
                viewModel.markVaultsInfoSeen(uid)
            }
        }
    }

    if (showVaultsInfoSheet) {
        VaultsInfoSheet(onDismiss = { showVaultsInfoSheet = false })
    }

    if (showVaultsListSettingsSheet) {
        VaultsListSettingsSheet(
            onVaultSettings = { onNavigate(AppScreenEnum.VAULT_SETTINGS) },
            onVaultsInfo = { showVaultsInfoSheet = true },
            onLearnMore = { com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "vaults") },
            onDismiss = { showVaultsListSettingsSheet = false }
        )
    }

    BaseScreen(
        title = Loc.Vaults.vaults,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(VaultsEvent.Refresh) },
        isSearchVisible = uiState.isSearchVisible,
        searchQuery = draftSearchQuery,
        onSearchQueryChange = { draftSearchQuery = it },
        onSearchClose = {
            viewModel.onEvent(VaultsEvent.UpdateSearchQuery(""))
            viewModel.onEvent(VaultsEvent.ToggleSearch)
        },
        onSearchTriggered = {
            viewModel.onEvent(VaultsEvent.UpdateSearchQuery(draftSearchQuery))
        },
        actions = {
            IconButton(onClick = { viewModel.onEvent(VaultsEvent.ToggleSearch) }) {
                Icon(Icons.Default.Search, null)
            }
            IconButton(onClick = { showVaultsListSettingsSheet = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { showCreateSheet = true },
                contentDescription = Loc.Vaults.createVault
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (uiState.isLoading) {
                // Keep empty while loading
            } else if (uiState.filteredVaults.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(Loc.Vaults.noVaults, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pagedVaults, key = { it.id }) { vault ->
                        val ownerName = uiState.userNames[vault.createdBy] ?: Loc.General.unknown
                        val isOwner = vault.createdBy.isEmpty() || vault.createdBy == currentUserId || vault.createdBy == authUid || isAdmin

                        ActionListItem(
                            onClick = {
                                viewModel.onEvent(VaultsEvent.SelectVault(vault))
                                onNavigate(AppScreenEnum.VAULT_DETAILS)
                            },
                            headline = vault.name,
                            supportingText = if (vault.description.isNotBlank()) vault.description else "${Loc.General.by} $ownerName",
                            metadataText = if (vault.description.isNotBlank()) "${Loc.General.by} $ownerName" else null,
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (showDriveButtons && vault.driveFolderUrl.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vault.driveFolderUrl))
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = "Drive Folder")
                                        }
                                    }

                                    if (isOwner) {
                                        IconButton(onClick = { vaultForActionSheet = vault }) {
                                            Icon(Icons.Default.MoreVert, null)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    if (uiState.filteredVaults.size > pagedVaults.size) {
                        item {
                            TextButton(
                                onClick = {
                                    if (!isOnline) {
                                        infoMessage = "No Internet Connection. Connection required to load more vaults." to MessageType.INFO
                                    } else {
                                        viewModel.onEvent(VaultsEvent.LoadMore)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Text("Load More")
                            }
                        }
                    }

                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (vaultForActionSheet != null) {
        VaultItemActionSheet(
            vault = vaultForActionSheet!!,
            onEdit = {
                editingVault = vaultForActionSheet
                vaultForActionSheet = null
            },
            onDelete = {
                warningDeleteVault = vaultForActionSheet
                vaultForActionSheet = null
            },
            onDismiss = { vaultForActionSheet = null }
        )
    }

    if (showCreateSheet) {
        CreateVaultSheet(
            onDismiss = { showCreateSheet = false },
            onConfirm = { name, desc ->
                viewModel.onEvent(VaultsEvent.CreateVault(name, desc) { success, errorMsg ->
                    if (!success) {
                        infoMessage = (errorMsg ?: Loc.Vaults.driveFolderError) to MessageType.ERROR
                    } else {
                        showCreateSheet = false
                    }
                })
            }
        )
    }

    if (editingVault != null) {
        CreateVaultSheet(
            editingVault = editingVault,
            onDismiss = { editingVault = null },
            onConfirm = { name, desc ->
                viewModel.onEvent(VaultsEvent.UpdateVault(editingVault!!.copy(name = name, description = desc)))
            }
        )
    }

    if (warningDeleteVault != null) {
        VaultDeleteWarningSheet(
            vault = warningDeleteVault!!,
            onDismiss = { warningDeleteVault = null },
            onProceedToConfirm = {
                confirmDeleteVault = warningDeleteVault
                warningDeleteVault = null
            }
        )
    }

    if (confirmDeleteVault != null) {
        VaultDeleteConfirmSheet(
            vault = confirmDeleteVault!!,
            onDismiss = { confirmDeleteVault = null },
            onConfirmDelete = {
                val v = confirmDeleteVault
                confirmDeleteVault = null
                if (v != null) {
                    viewModel.onEvent(VaultsEvent.DeleteVault(v.id) { success ->
                        if (success) {
                            infoMessage = "Vault Deleted" to MessageType.SUCCESS
                        } else {
                            infoMessage = "Failed to delete vault." to MessageType.ERROR
                        }
                    })
                }
            }
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
