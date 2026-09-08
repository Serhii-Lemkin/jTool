package com.jtool.android.features.calendar.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.jtool.android.core.components.layout.GenericTitle
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
fun CalendarAddEditScreen(
    event: CalendarEvent?,
    selectedDate: LocalDate,
    onBack: () -> Unit,
    onConfirm: (CalendarEvent, List<String>) -> Unit,
    settingsManager: SettingsManager
) {
    val groups = remember { settingsManager.getGroups() }
    
    var title by remember { mutableStateOf(event?.title ?: "") }
    var note by remember { mutableStateOf(event?.note ?: "") }
    var startTime by remember { mutableStateOf(event?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(event?.endTime ?: "") }
    var allDay by remember { mutableStateOf(event?.allDay ?: false) }
    var type by remember { mutableStateOf(event?.type ?: CalendarEventTypeEnum.COMMON) }
    var status by remember { mutableStateOf(event?.status ?: CalendarEventStatusEnum.REGULAR) }
    
    val initialSelectedGroups = remember { event?.sharedGroupIds?.toSet() ?: emptySet() }
    val selectedGroupIds = remember { mutableStateListOf<String>().apply { addAll(initialSelectedGroups) } }
    
    var syncToGoogle by remember { mutableStateOf(settingsManager.isGoogleCalendarSyncEnabled() && settingsManager.isGoogleCalendarPushEnabled()) }

    var showGroupPicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var pickingStartTime by remember { mutableStateOf<Boolean?>(null) }

    val themeColor = if (type == CalendarEventTypeEnum.SHIFT) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary

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
        
        ModalBottomSheet(
            onDismissRequest = { pickingStartTime = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (pickingStartTime == true) Loc.Workday.selectStartTime else Loc.Workday.selectTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))
                TimePicker(state = timePickerState)
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    val newTime = String.format(Locale.ENGLISH, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                    if (pickingStartTime == true) startTime = newTime else endTime = newTime
                    pickingStartTime = null
                }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(Loc.General.select)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            GenericTitle(
                title = if (event == null) Loc.Calendar.addEvent else Loc.Calendar.editEvent,
                onBack = onBack,
                actions = {
                    IconButton(
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
                                    allDay = allDay,
                                    note = note,
                                    status = status,
                                    ownerUid = settingsManager.getUserId(),
                                    dtStart = dtStart,
                                    dtEnd = dtEnd,
                                    googleCalendarId = event?.googleCalendarId,
                                    googleEventId = event?.googleEventId,
                                    sharedGroupIds = selectedGroupIds.toList()
                                ),
                                selectedGroupIds.toList()
                            )
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, null, tint = themeColor)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title & Type Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(Loc.Calendar.eventTitle) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            focusedLabelColor = themeColor
                        )
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTypePicker = true }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (type == CalendarEventTypeEnum.SHIFT) Icons.Default.Work else Icons.Default.Category, null, tint = themeColor)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (type == CalendarEventTypeEnum.SHIFT) Loc.Calendar.shift else Loc.Calendar.common,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null, tint = themeColor)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Time & Date Card
            Text(
                text = Loc.Calendar.time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.Calendar.allDay, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = allDay, onCheckedChange = { allDay = it }, colors = SwitchDefaults.colors(checkedThumbColor = themeColor, checkedTrackColor = themeColor.copy(alpha = 0.5f)))
                    }

                    if (!allDay) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TimeButton(
                                label = Loc.Workday.start,
                                time = startTime,
                                onClick = { pickingStartTime = true },
                                modifier = Modifier.weight(1f)
                            )
                            TimeButton(
                                label = Loc.Workday.finish,
                                time = if (endTime.isEmpty()) Loc.General.none else endTime,
                                onClick = { pickingStartTime = false },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Visibility Card
            Text(
                text = Loc.Calendar.belongsTo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ActionListItem(
                        headline = Loc.Calendar.shareWithGroups,
                        supportingText = if (selectedGroupIds.isEmpty()) {
                            Loc.Calendar.private
                        } else {
                            selectedGroupIds.map { id ->
                                val group = groups[id]
                                if (group?.isPrivate == true) Loc.Calendar.private else group?.name ?: id
                            }.joinToString(", ")
                        },
                        leadingContent = { Icon(Icons.Default.Groups, null, tint = themeColor) },
                        trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                        onClick = { showGroupPicker = true },
                        supportingMaxLines = 1
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = syncToGoogle, 
                            onCheckedChange = { syncToGoogle = it },
                            colors = CheckboxDefaults.colors(checkedColor = themeColor)
                        )
                        Text(Loc.Calendar.syncSubtitle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Note Card
            Text(
                text = Loc.Calendar.note,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(Loc.Calendar.note) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColor,
                    focusedLabelColor = themeColor
                )
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TimeButton(label: String, time: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(time, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}
