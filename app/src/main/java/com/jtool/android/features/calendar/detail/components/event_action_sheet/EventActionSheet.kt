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
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun EventActionSheet(
    event: CalendarEvent,
    canEdit: Boolean,
    canChangeRsvp: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChangeRsvp: () -> Unit = {},
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
            if (canChangeRsvp) {
                ActionListItem(
                    headline = CalendarLoc.changeRsvpStatus,
                    leadingContent = { Icon(Icons.Default.EventAvailable, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { onChangeRsvp(); onDismiss() }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }

            if (canEdit) {
                ActionListItem(
                    headline = Loc.General.edit,
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    onClick = { onEdit(); onDismiss() }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
            
            ActionListItem(
                headline = Loc.General.delete,
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { onDelete(); onDismiss() },
                headlineColor = MaterialTheme.colorScheme.error
            )
        }
    }
}
