package com.jtool.android.features.calendar.components.event_type_selection_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc

enum class CalendarCreationType {
    EVENT,
    REMINDER,
    WORK_SHIFT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeSelectionSheet(
    onSelectType: (CalendarCreationType) -> Unit,
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
            Text(
                text = Loc.s("calendar_createItemTitle", "Create New Item"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ActionListItem(
                headline = Loc.Calendar.event,
                supportingText = Loc.s("calendar_createEventSubtitle", "Standard Google Calendar event with full details"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    onSelectType(CalendarCreationType.EVENT)
                    onDismiss()
                }
            )

            ActionListItem(
                headline = Loc.s("calendar_createReminder", "Reminder"),
                supportingText = Loc.s("calendar_createReminderSubtitle", "Quick timed reminder with notification alert"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    onSelectType(CalendarCreationType.REMINDER)
                    onDismiss()
                }
            )

            ActionListItem(
                headline = Loc.s("calendar_createWorkShift", "Work Shift"),
                supportingText = Loc.s("calendar_createWorkShiftSubtitle", "Pre-filled work shift with custom shift settings"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    onSelectType(CalendarCreationType.WORK_SHIFT)
                    onDismiss()
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
