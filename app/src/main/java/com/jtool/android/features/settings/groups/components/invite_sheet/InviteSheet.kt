package com.jtool.android.features.settings.groups.components.invite_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun InviteSheet(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inviteEmail by remember { mutableStateOf("") }
    
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Settings.groupInvite,
        showCloseButton = false
    ) {
        Text(
            text = Loc.Settings.enterEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = inviteEmail,
            onValueChange = { inviteEmail = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
            )
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = { onConfirm(inviteEmail) },
            enabled = inviteEmail.contains("@"),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(Loc.Support.submit)
        }
        

    }
}
