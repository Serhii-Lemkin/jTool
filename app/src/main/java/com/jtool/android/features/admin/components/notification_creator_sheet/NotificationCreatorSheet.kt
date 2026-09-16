package com.jtool.android.features.admin.components.notification_creator_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet
import com.jtool.android.core.models.common.enums.NotificationTypeEnum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCreatorSheet(
    targetName: String,
    onDismiss: () -> Unit,
    onSend: (String, String, NotificationTypeEnum) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NotificationTypeEnum.INFO) }
    var typeExpanded by remember { mutableStateOf(false) }

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Admin.sendNotification,
        showCloseButton = false
    ) {
        Text(
            text = Loc.Admin.toTarget.format(targetName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedType.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Notification Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                NotificationTypeEnum.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            selectedType = type
                            typeExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(Loc.Admin.notificationTitle) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text(Loc.Admin.notificationMessage) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onSend(title, message, selectedType) },
            enabled = title.isNotBlank() && message.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(Loc.Admin.sendNotification)
        }

    }
}
