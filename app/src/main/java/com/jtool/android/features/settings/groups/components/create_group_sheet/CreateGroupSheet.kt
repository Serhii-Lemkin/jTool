package com.jtool.android.features.settings.groups.components.create_group_sheet

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
fun CreateGroupSheet(
    onDismiss: () -> Unit,
    onConfirm: (groupName: String, isInviteOnly: Boolean) -> Unit
) {
    var groupNameInput by remember { mutableStateOf("") }
    var isInviteOnlyInput by remember { mutableStateOf(false) }
    
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Settings.createNewGroup,
        showCloseButton = false
    ) {
        OutlinedTextField(
            value = groupNameInput,
            onValueChange = { groupNameInput = it },
            label = { Text(Loc.Settings.groupName) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Loc.Settings.inviteOnly,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = Loc.Settings.inviteOnlyDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = isInviteOnlyInput,
                onCheckedChange = { isInviteOnlyInput = it }
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onConfirm(groupNameInput, isInviteOnlyInput) },
            enabled = groupNameInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(Loc.General.save) }

    }
}
