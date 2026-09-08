package com.jtool.android.features.calendar.detail.components.event_action_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun EventActionSheet(
    event: CalendarEvent,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSyncToggle: () -> Unit,
    onShareToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Calendar.event,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            if (canEdit) {
                ActionListItem(
                    headline = Loc.General.edit,
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    onClick = { onEdit(); onDismiss() }
                )
            }
            
            ActionListItem(
                headline = if (event.googleEventId != null) Loc.Calendar.removeFromGoogle else Loc.Calendar.syncToGoogle,
                leadingContent = { Icon(if (event.googleEventId != null) Icons.Default.SyncDisabled else Icons.Default.Sync, null) },
                onClick = { onSyncToggle(); onDismiss() }
            )

            if (canEdit) {
                ActionListItem(
                    headline = Loc.Calendar.shareWithGroups,
                    leadingContent = { Icon(Icons.Default.Share, null) },
                    onClick = { onShareToggle() }
                )
            }

            if (canEdit) {
                ActionListItem(
                    headline = Loc.General.delete,
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { onDelete(); onDismiss() },
                    headlineColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
