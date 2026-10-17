package com.jtool.android.features.vaults.components.vault_delete_confirm_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun VaultDeleteConfirmSheet(
    vault: Vault,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    var typedName by remember { mutableStateOf("") }
    val isMatch = typedName.trim().equals(vault.name.trim(), ignoreCase = true)

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
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(52.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "${Loc.General.delete} \"${vault.name}\"",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = Loc.Vaults.typeVaultNameToDelete,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = typedName,
                onValueChange = { typedName = it },
                placeholder = { Text(vault.name) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onConfirmDelete,
                enabled = isMatch,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(Loc.General.delete, fontWeight = FontWeight.Bold)
            }
        }
    }
}
