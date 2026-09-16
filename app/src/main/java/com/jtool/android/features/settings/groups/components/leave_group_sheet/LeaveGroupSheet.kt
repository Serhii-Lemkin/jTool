package com.jtool.android.features.settings.groups.components.leave_group_sheet

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
fun LeaveGroupSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLastMember: Boolean = false
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = if (isLastMember) Loc.Settings.deleteGroupTitle else Loc.Settings.confirmUnlinkTitle,
        showCloseButton = false
    ) {
        Icon(
            Icons.Default.Warning,
            null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isLastMember) Loc.Settings.lastMemberLeaveMsg else Loc.Settings.confirmUnlinkMsg,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(Loc.Settings.unlinkGroup) }

    }
}
