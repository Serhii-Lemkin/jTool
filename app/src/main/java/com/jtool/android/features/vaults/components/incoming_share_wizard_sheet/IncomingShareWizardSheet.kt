package com.jtool.android.features.vaults.components.incoming_share_wizard_sheet

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.AttachedFile
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.components.upload_doc_sheet.UploadDocSheet
import kotlinx.coroutines.launch

enum class ShareWizardStep {
    GROUP_SELECT,
    APP_SELECT,
    VAULT_SELECT,
    UPLOAD_FORM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingShareWizardSheet(
    sharedUris: List<Uri>,
    groups: Map<String, GroupInfo>,
    currentGroupId: String?,
    dataManager: DataManager,
    onConfirmUpload: (groupId: String, vault: Vault, docName: String, tag: String, docDate: Long, description: String, saveOffline: Boolean, files: List<AttachedFile>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(ShareWizardStep.GROUP_SELECT) }
    
    var selectedGroupId by remember { mutableStateOf(currentGroupId ?: groups.keys.firstOrNull() ?: "") }
    var selectedVault by remember { mutableStateOf<Vault?>(null) }
    
    val availableVaults by remember(selectedGroupId) {
        dataManager.getVaultsFlow(selectedGroupId)
    }.collectAsState(initial = emptyList())

    var showCreateVaultDialog by remember { mutableStateOf(false) }
    var newVaultNameInput by remember { mutableStateOf("") }

    if (currentStep == ShareWizardStep.UPLOAD_FORM && selectedVault != null) {
        val attachedFiles = remember(sharedUris) {
            sharedUris.map { uri ->
                val fileName = VaultDocUtils.getFileNameFromUri(context, uri)
                val ext = VaultDocUtils.getExtFromUri(context, uri)
                AttachedFile(
                    uri = uri,
                    fileName = fileName,
                    extension = ext
                )
            }
        }
        val firstFileName = remember(attachedFiles) {
            attachedFiles.firstOrNull()?.fileName ?: ""
        }

        UploadDocSheet(
            vault = selectedVault!!,
            initialFiles = attachedFiles,
            initialRecordName = firstFileName,
            onDismiss = onDismiss,
            onUpload = { files, docName, tag, docDate, desc, saveOffline ->
                onConfirmUpload(
                    selectedGroupId,
                    selectedVault!!,
                    docName,
                    tag,
                    docDate,
                    desc,
                    saveOffline,
                    files
                )
            }
        )
        return
    }

    androidx.activity.compose.BackHandler(enabled = currentStep != ShareWizardStep.GROUP_SELECT) {
        currentStep = when (currentStep) {
            ShareWizardStep.APP_SELECT -> ShareWizardStep.GROUP_SELECT
            ShareWizardStep.VAULT_SELECT -> ShareWizardStep.APP_SELECT
            ShareWizardStep.UPLOAD_FORM -> ShareWizardStep.VAULT_SELECT
            else -> ShareWizardStep.GROUP_SELECT
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep != ShareWizardStep.GROUP_SELECT) {
                    IconButton(
                        onClick = {
                            currentStep = when (currentStep) {
                                ShareWizardStep.APP_SELECT -> ShareWizardStep.GROUP_SELECT
                                ShareWizardStep.VAULT_SELECT -> ShareWizardStep.APP_SELECT
                                else -> ShareWizardStep.GROUP_SELECT
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                val titleText = when (currentStep) {
                    ShareWizardStep.GROUP_SELECT -> Loc.s("vaults_wizardGroupTitle", "Step 1: Select Group")
                    ShareWizardStep.APP_SELECT -> Loc.s("vaults_wizardAppTitle", "Step 2: Select Feature App")
                    ShareWizardStep.VAULT_SELECT -> Loc.s("vaults_wizardVaultTitle", "Step 3: Select Target Vault")
                    else -> Loc.s("vaults_wizardUploadTitle", "Step 4: Upload Shared Files")
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            when (currentStep) {
                ShareWizardStep.GROUP_SELECT -> {
                    Text(
                        text = Loc.s("vaults_wizardSelectGroupSubtitle", "Choose which group to share these files with:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(groups.values.toList(), key = { it.id }) { group ->
                            val isSelected = group.id == selectedGroupId
                            ActionListItem(
                                headline = if (group.isPrivate) Loc.s("calendar_private", "Private / Me") else group.name,
                                leadingContent = {
                                    Icon(
                                        imageVector = if (group.isPrivate) Icons.Default.Person else Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    selectedGroupId = group.id
                                    currentStep = ShareWizardStep.APP_SELECT
                                }
                            )
                        }
                    }
                }

                ShareWizardStep.APP_SELECT -> {
                    Text(
                        text = Loc.s("vaults_wizardSelectAppSubtitle", "Select destination feature app:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ActionListItem(
                        headline = Loc.s("vaults_title", "Vaults"),
                        supportingText = Loc.s("vaults_appSubtitle", "Secured document and file storage"),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            currentStep = ShareWizardStep.VAULT_SELECT
                        }
                    )
                }

                ShareWizardStep.VAULT_SELECT -> {
                    Text(
                        text = Loc.s("vaults_wizardSelectVaultSubtitle", "Select an existing vault or create a new one:"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ActionListItem(
                        headline = Loc.s("vaults_createNewVault", "+ Create New Vault"),
                        supportingText = Loc.s("vaults_createNewVaultSubtitle", "Add a new vault for this group on the fly"),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            newVaultNameInput = ""
                            showCreateVaultDialog = true
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 260.dp)
                    ) {
                        items(availableVaults, key = { it.id }) { vault ->
                            val isSelected = vault.id == selectedVault?.id
                            ActionListItem(
                                headline = vault.name,
                                supportingText = vault.description.ifBlank { null },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    selectedVault = vault
                                    currentStep = ShareWizardStep.UPLOAD_FORM
                                }
                            )
                        }
                    }
                }

                else -> {}
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCreateVaultDialog) {
        AlertDialog(
            onDismissRequest = { showCreateVaultDialog = false },
            title = { Text(Loc.s("vaults_createNewVault", "Create New Vault")) },
            text = {
                OutlinedTextField(
                    value = newVaultNameInput,
                    onValueChange = { newVaultNameInput = it },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                    trailingIcon = {
                        if (newVaultNameInput.isNotBlank()) {
                            IconButton(onClick = { newVaultNameInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    label = { Text(Loc.s("vaults_vaultName", "Vault Name")) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newVaultNameInput.isNotBlank()) {
                            scope.launch {
                                val newVault = Vault(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = newVaultNameInput.trim(),
                                    timestamp = System.currentTimeMillis()
                                )
                                val createdId = dataManager.addVault(selectedGroupId, newVault)
                                val finalVault = newVault.copy(id = createdId)
                                selectedVault = finalVault
                                showCreateVaultDialog = false
                                currentStep = ShareWizardStep.UPLOAD_FORM
                            }
                        }
                    }
                ) { Text(Loc.General.save) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateVaultDialog = false }) {
                    Text(Loc.General.cancel)
                }
            }
        )
    }
}
