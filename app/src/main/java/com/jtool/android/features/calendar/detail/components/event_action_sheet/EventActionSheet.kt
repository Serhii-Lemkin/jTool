package com.jtool.android.features.calendar.detail.components.event_action_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.core.models.calendar.CalendarEvent

@OptIn(ExperimentalMaterial3Api::class)
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (canChangeRsvp) {
                ActionListItem(
                    headline = CalendarLoc.changeRsvpStatus,
                    supportingText = Loc.s("calendar_changeRsvpSubtitle", "Accept or decline your invitation to this event"),
                    leadingContent = { Icon(Icons.Default.EventAvailable, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { onChangeRsvp(); onDismiss() }
                )
            }

            if (canEdit) {
                ActionListItem(
                    headline = Loc.s("calendar_editEventTitle", "Edit Event"),
                    supportingText = Loc.s("calendar_editEventSubtitle", "Modify title, date, time, location, or notes"),
                    leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { onEdit(); onDismiss() }
                )
            }

            ActionListItem(
                headline = Loc.s("calendar_deleteEventTitle", "Delete Event"),
                supportingText = Loc.s("calendar_deleteEventSubtitle", "Permanently remove this event from your calendar"),
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { onDelete(); onDismiss() },
                headlineColor = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
