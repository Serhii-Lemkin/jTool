package com.jtool.android.features.workday.tracker.components.today_events_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getEventColor
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun TodayEventsSheet(
    events: List<CalendarEvent>,
    selectedEventId: String?,
    onSelectEvent: (CalendarEvent) -> Unit,
    onDismiss: () -> Unit,
    workShiftColorKey: String = "1"
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Workday.selectEvent,
        showCloseButton = false
    ) {
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Loc.Calendar.noEvents,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.Top
            ) {
                items(events, key = { it.id }) { event ->
                    val isSelected = selectedEventId == event.id
                    val themeColor = getEventColor(event.colorKey)
                    val isWork = (event.colorKey != null && event.colorKey == workShiftColorKey) || (event.colorKey == null && workShiftColorKey == "1")

                    ActionListItem(
                        headline = event.title,
                        supportingText = "${event.startTime} - ${event.endTime}",
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(themeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWork) Icons.Default.Work else Icons.Default.Event,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = themeColor
                                )
                            }
                        },
                        onClick = {
                            onSelectEvent(event)
                            onDismiss()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
            }
        }
    }
}
