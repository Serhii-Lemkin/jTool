package com.jtool.android.features.settings.groups.components.edit_group_name_sheet

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
fun EditGroupNameSheet(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newGroupName by remember { mutableStateOf(initialName) }
    
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Settings.editGroupName,
        showCloseButton = false
    ) {
        OutlinedTextField(
            value = newGroupName,
            onValueChange = { newGroupName = it },
            label = { Text(Loc.Settings.groupName) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onConfirm(newGroupName) },
            enabled = newGroupName.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(Loc.General.save)
        }

    }
}
