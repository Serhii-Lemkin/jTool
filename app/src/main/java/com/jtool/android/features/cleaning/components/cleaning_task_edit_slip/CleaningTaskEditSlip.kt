package com.jtool.android.features.cleaning.components.cleaning_task_edit_slip

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningTaskEditSlip(
    title: String,
    days: String,
    isRecurring: Boolean,
    isEdit: Boolean,
    onTitleChange: (String) -> Unit,
    onDaysChange: (String) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEdit) Loc.cleaning_editTask else Loc.cleaning_addTask,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(Loc.cleaning_taskTitle) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = days,
                onValueChange = { if (it.all { c -> c.isDigit() }) onDaysChange(it) },
                label = { Text(Loc.cleaning_everyXDays) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(Loc.cleaning_recurring, style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isRecurring, onCheckedChange = onRecurringChange)
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onConfirm,
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (isEdit) Loc.general_save else Loc.general_add)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(Loc.general_cancel)
            }
        }
    }
}
