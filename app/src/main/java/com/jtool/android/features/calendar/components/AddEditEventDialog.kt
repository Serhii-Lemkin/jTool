package com.jtool.android.features.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventStatusEnum
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.calendar.components.group_picker_sheet.GroupPickerSheet
import com.jtool.android.features.calendar.components.type_picker_sheet.TypePickerSheet
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventDialog(
    event: CalendarEvent?,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (CalendarEvent, List<String>) -> Unit,
    settingsManager: SettingsManager
) {
    val groups = remember { settingsManager.getGroups() }
    
    var title by remember { mutableStateOf(event?.title ?: "") }
    var note by remember { mutableStateOf(event?.note ?: "") }
    var startTime by remember { mutableStateOf(event?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(event?.endTime ?: "") }
    var type by remember { mutableStateOf(event?.type ?: CalendarEventTypeEnum.COMMON) }
    var status by remember { mutableStateOf(event?.status ?: CalendarEventStatusEnum.REGULAR) }
    
    val initialSelectedGroups = remember { event?.sharedGroupIds?.toSet() ?: emptySet() }
    val selectedGroupIds = remember { mutableStateListOf<String>().apply { addAll(initialSelectedGroups) } }
    
    var showGroupPicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var pickingStartTime by remember { mutableStateOf<Boolean?>(null) }

    if (showGroupPicker) {
        GroupPickerSheet(
            groups = groups,
            selectedIds = selectedGroupIds.toSet(),
            onToggleId = { id ->
                if (selectedGroupIds.contains(id)) {
                    selectedGroupIds.remove(id)
                } else {
                    selectedGroupIds.add(id)
                }
            },
            onDismiss = { showGroupPicker = false }
        )
    }

    if (showTypePicker) {
        TypePickerSheet(
            selectedType = type,
            onTypeSelected = { type = it },
            onDismiss = { showTypePicker = false }
        )
    }

    if (pickingStartTime != null) {
        val initialTime = if (pickingStartTime == true) startTime else (if (endTime.isEmpty()) "10:00" else endTime)
        val parsedTime = try { LocalTime.parse(initialTime) } catch (_: Exception) { LocalTime.of(9, 0) }
        val timePickerState = rememberTimePickerState(
            initialHour = parsedTime.hour,
            initialMinute = parsedTime.minute,
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { pickingStartTime = null },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = String.format(Locale.ENGLISH, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                    if (pickingStartTime == true) startTime = newTime else endTime = newTime
                    pickingStartTime = null
                }) { Text(Loc.General.select) }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (pickingStartTime == true) Loc.Workday.selectStartTime else Loc.Workday.selectTime,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event == null) Loc.Calendar.addEvent else Loc.Calendar.editEvent) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Loc.Calendar.eventTitle) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { pickingStartTime = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Loc.Workday.start, style = MaterialTheme.typography.labelSmall)
                            Text(startTime)
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { pickingStartTime = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Loc.Workday.finish, style = MaterialTheme.typography.labelSmall)
                            Text(if (endTime.isEmpty()) Loc.General.none else endTime)
                        }
                    }
                }

                ListItem(
                    headlineContent = { Text(Loc.Calendar.eventType) },
                    supportingContent = { Text(if (type == CalendarEventTypeEnum.SHIFT) Loc.Calendar.shift else Loc.Calendar.common) },
                    trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.clickable { showTypePicker = true }
                )

                ListItem(
                    headlineContent = { Text(Loc.Calendar.selectGroups) },
                    supportingContent = { 
                        Text(
                            if (selectedGroupIds.isEmpty()) Loc.Calendar.private 
                            else selectedGroupIds.map { id -> groups[id]?.name ?: id }.joinToString(", ")
                        ) 
                    },
                    trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.clickable { showGroupPicker = true }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Loc.Calendar.note) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startLdt = LocalDate.parse(selectedDate.toString()).atTime(LocalTime.parse(startTime))
                    val endLdt = if (endTime.isNotEmpty()) LocalDate.parse(selectedDate.toString()).atTime(LocalTime.parse(endTime)) else startLdt.plusHours(1)
                    
                    val dtStart = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val dtEnd = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
                            ownerUid = settingsManager.getUserId(),
                            dtStart = dtStart,
                            dtEnd = dtEnd,
                            sharedGroupIds = selectedGroupIds.toList()
                        ),
                        selectedGroupIds.toList()
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text(Loc.General.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Loc.General.cancel)
            }
        }
    )
}
