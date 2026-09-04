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
    onConfirm: (String) -> Unit
) {
    var groupNameInput by remember { mutableStateOf("") }
    
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
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onConfirm(groupNameInput) },
            enabled = groupNameInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(Loc.General.save) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(Loc.General.cancel)
        }
    }
}
