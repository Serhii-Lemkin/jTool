package com.jtool.android.features.vaults.components.create_vault_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun CreateVaultSheet(
    editingVault: Vault? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(editingVault?.name ?: "") }
    var description by remember { mutableStateOf(editingVault?.description ?: "") }

    GenericSheet(
        onDismiss = onDismiss,
        title = if (editingVault == null) Loc.Vaults.createVault else Loc.General.edit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(Loc.Vaults.vaultName) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(Loc.Vaults.description) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim())
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(Loc.General.save, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
