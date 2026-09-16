package com.jtool.android.features.cleaning.components.cleaning_task_edit_sheet

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
fun CleaningTaskEditSheet(
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
    GenericSheet(
        onDismiss = onDismiss,
        title = if (isEdit) Loc.Cleaning.editTask else Loc.Cleaning.addTask,
        showCloseButton = false
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(Loc.Cleaning.taskTitle) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = days,
            onValueChange = { if (it.all { c -> c.isDigit() }) onDaysChange(it) },
            label = { Text(Loc.Cleaning.everyXDays) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(Loc.Cleaning.recurring, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isRecurring, onCheckedChange = onRecurringChange)
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onConfirm,
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if (isEdit) Loc.General.save else Loc.General.add)
        }

    }
}
