package com.jtool.android.features.calendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventStatusEnum
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventDialog(
    event: CalendarEvent?,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (CalendarEvent, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var note by remember { mutableStateOf(event?.note ?: "") }
    var startTime by remember { mutableStateOf(event?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(event?.endTime ?: "") }
    var type by remember { mutableStateOf(event?.type ?: CalendarEventTypeEnum.COMMON) }
    var status by remember { mutableStateOf(event?.status ?: CalendarEventStatusEnum.REGULAR) }
    var isShared by remember { mutableStateOf(event?.groupsVisibility?.isNotEmpty() ?: false) }

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
                text = if (event == null) Loc.Calendar.addEvent else Loc.Calendar.editEvent,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == CalendarEventTypeEnum.COMMON,
                    onClick = { type = CalendarEventTypeEnum.COMMON },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text(Loc.Calendar.common) }
                SegmentedButton(
                    selected = type == CalendarEventTypeEnum.SHIFT,
                    onClick = { type = CalendarEventTypeEnum.SHIFT },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text(Loc.Calendar.shift) }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(Loc.Calendar.eventTitle) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTime, 
                    onValueChange = { startTime = it }, 
                    label = { Text(Loc.Workday.start) }, 
                    placeholder = { Text("09:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endTime, 
                    onValueChange = { endTime = it }, 
                    label = { Text(Loc.Workday.finish) }, 
                    placeholder = { Text("18:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(Loc.Calendar.note) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onConfirm(
                        CalendarEvent(
                            id = event?.id ?: "",
                            title = title,
                            type = type,
                            date = selectedDate.toString(),
                            startTime = startTime,
                            endTime = endTime.ifEmpty { null },
                            note = note,
                            status = status,
                            groupsVisibility = if (isShared) listOf("shared") else emptyList()
                        ),
                        isShared
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(Loc.General.save)
            }
        }
    }
}
