package com.jtool.android.features.calendar.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.calendar.components.group_picker_sheet.GroupPickerSheet
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.EventColorPickerSheet
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getGoogleCalendarColorPalette
import com.jtool.android.features.calendar.edit.components.required_fields_sheet.RequiredFieldsSheet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAddEditScreen(
    event: CalendarEvent?,
    extension: CalendarEventExtension?,
    selectedDate: LocalDate,
    onBack: () -> Unit,
    onConfirm: (CalendarEvent, CalendarEventExtension) -> Unit,
    settingsManager: SettingsManager,
    groups: Map<String, GroupInfo>
) {
    val stateKey = remember(event?.id, selectedDate) { Any() }

    var title by remember(stateKey) { mutableStateOf(event?.title ?: "") }
    var note by remember(stateKey) { mutableStateOf(event?.note ?: "") }
    var startTime by remember(stateKey) { mutableStateOf(if (event?.startTime.isNullOrBlank()) "09:00" else event!!.startTime) }
    var endTime by remember(stateKey) { mutableStateOf(event?.endTime ?: "") }
    var allDay by remember(stateKey) { mutableStateOf(event?.allDay ?: false) }
    var currentDate by remember(stateKey) { 
        mutableStateOf(
            if (event?.date != null && event.date.isNotEmpty()) LocalDate.parse(event.date) else selectedDate
        ) 
    }
    
    var colorKey by remember(stateKey) { mutableStateOf(event?.colorKey ?: "7") }
    
    val selectedGroupIds = remember(stateKey) { mutableStateListOf<String>().apply { addAll(extension?.sharedGroupIds ?: emptyList()) } }

    var showGroupPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showRequiredFieldsSheet by remember { mutableStateOf(false) }
    var pickingStartTime by remember { mutableStateOf<Boolean?>(null) }

    val themeColor = MaterialTheme.colorScheme.primary

    if (showRequiredFieldsSheet) {
        RequiredFieldsSheet(onDismiss = { showRequiredFieldsSheet = false })
    }

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

    if (showColorPicker) {
        EventColorPickerSheet(
            selectedColorKey = colorKey,
            onSelectColor = { colorOption ->
                colorKey = colorOption.key
            },
            onDismiss = { showColorPicker = false }
        )
    }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        currentDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(Loc.General.select) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(Loc.General.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                            if (title.trim().isEmpty()) {
                                showRequiredFieldsSheet = true
                                return@IconButton
                            }

                            val (dtStart, dtEnd) = if (allDay) {
                                val startUtc = currentDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                                val endUtc = currentDate.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                                startUtc to endUtc
                            } else {
                                val effectiveStart = if (startTime.isBlank()) "09:00" else startTime
                                val startLdt = currentDate.atTime(LocalTime.parse(effectiveStart))
                                val endLdt = if (endTime.isNotEmpty()) currentDate.atTime(LocalTime.parse(endTime)) else startLdt.plusHours(1)
                                val startMs = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                val endMs = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                startMs to endMs
                            }

                            val finalEvent = CalendarEvent(
                                id = event?.id ?: "",
                                title = title.trim(),
                                allDay = allDay,
                                note = note,
                                dtStart = dtStart,
                                dtEnd = dtEnd,
                                colorKey = colorKey,
                                googleCalendarId = event?.googleCalendarId ?: settingsManager.getGoogleCalendarId()
                            )
                            
                            val finalExt = CalendarEventExtension(
                                googleEventId = event?.id ?: "",
                                sharedGroupIds = selectedGroupIds.toList(),
                                dateTimestamp = dtStart
                            )

                            onConfirm(finalEvent, finalExt)
                        },
                        enabled = true
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Loc.Calendar.eventTitle)
                                Text(" *", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            focusedLabelColor = themeColor
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                val colorPalette = getGoogleCalendarColorPalette()
                val currentColorOption = colorPalette.find { it.key == colorKey } ?: colorPalette.first()
                ActionListItem(
                    headline = Loc.Calendar.eventColor,
                    supportingText = currentColorOption.name,
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(currentColorOption.hexColor)
                        )
                    },
                    onClick = { showColorPicker = true }
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = Loc.Calendar.time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ActionListItem(
                        headline = Loc.Calendar.date,
                        supportingText = currentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        leadingContent = { Icon(Icons.Default.CalendarToday, null, tint = themeColor) },
                        onClick = { showDatePicker = true },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    HorizontalDivider(modifier = Modifier.alpha(0.1f))

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

            Text(
                text = Loc.Calendar.belongsTo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
                }
            }

            Spacer(Modifier.height(24.dp))

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
