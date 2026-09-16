package com.jtool.android.features.settings.groups.components.join_group_sheet

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
fun JoinGroupSheet(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var joinGroupId by remember { mutableStateOf("") }
    
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Settings.joinGroup,
        showCloseButton = false
    ) {
        OutlinedTextField(
            value = joinGroupId,
            onValueChange = { joinGroupId = it },
            label = { Text(Loc.Settings.groupIdToJoin) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = Loc.Settings.joinRequestExplForRequester,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onConfirm(joinGroupId) },
            enabled = joinGroupId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(Loc.Settings.sendRequest) }

    }
}
