package com.jtool.android.features.settings.account.components.delete_account_confirm_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun DeleteAccountConfirmSheet(
    currentUserEmail: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var emailInput by remember { mutableStateOf("") }

    GenericSheet(
        onDismiss = onDismiss,
        title = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (step == 1) Icons.Default.DeleteForever else Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = if (step == 1) Loc.Settings.confirmEmailTitle else Loc.General.areYouSure,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = if (step == 1) Loc.Settings.confirmEmailMsg.format(currentUserEmail) else Loc.Settings.deleteAccountPermanentMsg,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (step == 1) {
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(Loc.Settings.email) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (step == 1) step = 2 else onConfirm()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = step == 2 || emailInput.equals(currentUserEmail, ignoreCase = true),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (step == 1) Loc.Settings.verify else Loc.General.delete)
            }
            

        }
    }
}
