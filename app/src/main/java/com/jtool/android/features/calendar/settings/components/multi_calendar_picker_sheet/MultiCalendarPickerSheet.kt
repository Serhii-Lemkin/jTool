package com.jtool.android.features.calendar.settings.components.multi_calendar_picker_sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun MultiCalendarPickerSheet(
    availableCalendars: List<Pair<String, String>>,
    selectedCalendarIds: Set<String>,
    onSaveCalendarIds: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSelectedIds by remember(selectedCalendarIds) { mutableStateOf(selectedCalendarIds) }

    GenericSheet(
        onDismiss = {
            onSaveCalendarIds(currentSelectedIds)
            onDismiss()
        },
        title = CalendarLoc.displayedCalendars,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = CalendarLoc.displayedCalendarsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (availableCalendars.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(Loc.Calendar.noCalendarsFound)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(availableCalendars) { (id, name) ->
                        val isSelected = currentSelectedIds.contains(id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = currentSelectedIds.toMutableSet()
                                    if (isSelected) {
                                        if (next.size > 1) next.remove(id)
                                    } else {
                                        next.add(id)
                                    }
                                    currentSelectedIds = next
                                    onSaveCalendarIds(next)
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    val next = currentSelectedIds.toMutableSet()
                                    if (isSelected) {
                                        if (next.size > 1) next.remove(id)
                                    } else {
                                        next.add(id)
                                    }
                                    currentSelectedIds = next
                                    onSaveCalendarIds(next)
                                }
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
