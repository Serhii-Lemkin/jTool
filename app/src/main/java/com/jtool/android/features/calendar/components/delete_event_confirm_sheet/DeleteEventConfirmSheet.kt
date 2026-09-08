package com.jtool.android.features.calendar.components.delete_event_confirm_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun DeleteEventConfirmSheet(
    isSynced: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.General.areYouSure,
        showCloseButton = false
    ) {
        Icon(
            Icons.Default.Warning,
            null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        
        val msg = if (isSynced) {
            "${Loc.General.confirmDeleteMsg}\n\n${Loc.Calendar.deleteFromGoogleWarning}"
        } else {
            Loc.General.confirmDeleteMsg
        }

        Text(
            text = msg,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(Loc.General.delete)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(Loc.General.cancel)
        }
    }
}
