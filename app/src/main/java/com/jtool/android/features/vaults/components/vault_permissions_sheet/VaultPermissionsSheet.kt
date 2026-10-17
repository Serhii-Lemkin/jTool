package com.jtool.android.features.vaults.components.vault_permissions_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.ui.components.common.GenericSheet
import kotlinx.coroutines.launch

@Composable
fun VaultPermissionsSheet(
    vault: Vault,
    groupMemberEmails: List<String>,
    userNames: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var permissionsMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(vault.driveFolderId) {
        isLoading = true
        permissionsMap = GoogleDriveVaultHelper.getVaultPermissionsWithEmails(context, vault.driveFolderId)
        isLoading = false
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Vaults.checkPermissions
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (groupMemberEmails.isEmpty()) {
                Text(
                    text = "No group members found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                groupMemberEmails.forEach { email ->
                    val cleanEmail = email.lowercase().trim()
                    val hasAccess = permissionsMap[cleanEmail] == true
                    val displayName = userNames[cleanEmail] ?: userNames[email] ?: email.substringBefore("@")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = hasAccess,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    scope.launch {
                                        val success = GoogleDriveVaultHelper.grantPermissionToUser(
                                            context,
                                            vault.driveFolderId,
                                            email
                                        )
                                        if (success) {
                                            permissionsMap = permissionsMap + (cleanEmail to true)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
