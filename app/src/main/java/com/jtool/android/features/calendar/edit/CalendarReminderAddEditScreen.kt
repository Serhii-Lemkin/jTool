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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.utils.rememberCustomDatePickerState
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
fun CalendarReminderAddEditScreen(
    event: CalendarEvent?,
    selectedDate: LocalDate,
    onBack: () -> Unit,
    onConfirm: (CalendarEvent, CalendarEventExtension) -> Unit,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val stateKey = remember(event?.id, selectedDate) { Any() }

    var title by remember(stateKey) { mutableStateOf(event?.title ?: "") }
    var note by remember(stateKey) { mutableStateOf(event?.note ?: "") }
    var reminderTime by remember(stateKey) { mutableStateOf(if (event?.startTime.isNullOrBlank()) "09:00" else event!!.startTime) }
    var reminderDate by remember(stateKey) { 
        mutableStateOf(if (event?.date != null && event.date.isNotEmpty()) LocalDate.parse(event.date) else selectedDate) 
    }
    
    var colorKey by remember(stateKey) { mutableStateOf(event?.colorKey ?: "5") }
    var isRecurring by remember(stateKey) { mutableStateOf(!event?.rrule.isNullOrEmpty()) }
    var rrule by remember(stateKey) { mutableStateOf(event?.rrule ?: "") }
    var reminderMinutesInput by remember(stateKey) { mutableStateOf(event?.reminderMinutes ?: 15) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showRepeatFrequencySheet by remember { mutableStateOf(false) }
    var showNotificationLeadSheet by remember { mutableStateOf(false) }
    var showRequiredFieldsSheet by remember { mutableStateOf(false) }

    val themeColor = MaterialTheme.colorScheme.primary

    if (showRequiredFieldsSheet) {
        RequiredFieldsSheet(onDismiss = { showRequiredFieldsSheet = false })
    }

    if (showColorPicker) {
        EventColorPickerSheet(
            selectedColorKey = colorKey,
            onSelectColor = { colorOption -> colorKey = colorOption.key },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberCustomDatePickerState(
            initialSelectedDateMillis = reminderDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        reminderDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
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

    if (showNotificationLeadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationLeadSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Loc.s("calendar_notificationAlert", "Notification Alert"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val leadOptions = listOf(
                    0 to Loc.s("calendar_atTimeOfReminder", "At time of reminder"),
                    5 to Loc.s("calendar_5mBefore", "5 minutes before"),
                    10 to Loc.s("calendar_10mBefore", "10 minutes before"),
                    15 to Loc.s("calendar_15mBefore", "15 minutes before"),
                    30 to Loc.s("calendar_30mBefore", "30 minutes before"),
                    60 to Loc.s("calendar_1hBefore", "1 hour before"),
                    1440 to Loc.s("calendar_1dBefore", "1 day before")
                )

                leadOptions.forEach { (mins, label) ->
                    ActionListItem(
                        headline = label,
                        leadingContent = {
                            RadioButton(
                                selected = reminderMinutesInput == mins,
                                onClick = null
                            )
                        },
                        onClick = {
                            reminderMinutesInput = mins
                            showNotificationLeadSheet = false
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showRepeatFrequencySheet) {
        ModalBottomSheet(
            onDismissRequest = { showRepeatFrequencySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = CalendarLoc.repeatFrequency,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val options = listOf(
                    "FREQ=DAILY" to CalendarLoc.repeatEveryDay,
                    "FREQ=WEEKLY" to CalendarLoc.repeatEveryWeek,
                    "FREQ=MONTHLY" to CalendarLoc.repeatEveryMonth,
                    "FREQ=YEARLY" to CalendarLoc.repeatEveryYear
                )

                options.forEach { (freqRule, label) ->
                    ActionListItem(
                        headline = label,
                        leadingContent = {
                            RadioButton(
                                selected = rrule.contains(freqRule),
                                onClick = null
                            )
                        },
                        onClick = {
                            rrule = freqRule
                            showRepeatFrequencySheet = false
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showTimePicker) {
        val parsedTime = try { LocalTime.parse(reminderTime) } catch (_: Exception) { LocalTime.of(9, 0) }
        val timePickerState = rememberTimePickerState(
            initialHour = parsedTime.hour,
            initialMinute = parsedTime.minute,
            is24Hour = true
        )
        
        ModalBottomSheet(
            onDismissRequest = { showTimePicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Loc.s("calendar_selectReminderTime", "Select Reminder Time"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))
                TimePicker(state = timePickerState)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        reminderTime = String.format(Locale.ENGLISH, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(Loc.General.select)
                }
            }
        }
    }

    BaseScreen(
        title = if (event == null) Loc.s("calendar_addReminder", "Add Reminder") else Loc.s("calendar_editReminder", "Edit Reminder"),
        onBack = onBack,
        actions = {
            IconButton(
                onClick = {
                    if (title.trim().isEmpty()) {
                        showRequiredFieldsSheet = true
                        return@IconButton
                    }

                    val effectiveTime = if (reminderTime.isBlank()) "09:00" else reminderTime
                    val startLdt = reminderDate.atTime(LocalTime.parse(effectiveTime))
                    val endLdt = startLdt.plusMinutes(15)

                    val startMs = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMs = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val effectiveRrule = if (isRecurring) (if (rrule.isEmpty()) "FREQ=DAILY" else rrule) else null
                    val computedDuration = if (isRecurring) {
                        com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper.calculateRfcDuration(startMs, endMs, false)
                    } else null

                    val finalEvent = CalendarEvent(
                        id = event?.id ?: "",
                        title = title.trim(),
                        allDay = false,
                        note = note,
                        dtStart = startMs,
                        dtEnd = if (isRecurring) null else endMs,
                        colorKey = colorKey,
                        rrule = effectiveRrule,
                        duration = computedDuration,
                        reminderMinutes = reminderMinutesInput,
                        googleCalendarId = event?.googleCalendarId ?: settingsManager.getGoogleCalendarId()
                    )
                    
                    val finalExt = CalendarEventExtension(
                        googleEventId = event?.id ?: "",
                        sharedGroupIds = emptyList(),
                        dateTimestamp = startMs
                    )

                    onConfirm(finalEvent, finalExt)
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = Loc.General.save)
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 12.dp),
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
                                Text(Loc.s("calendar_reminderTitle", "Reminder Title"))
                                Text(" *", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        trailingIcon = {
                            if (title.isNotBlank()) {
                                IconButton(onClick = { title = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
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
                    headline = Loc.s("calendar_reminderColor", "Reminder Color"),
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
                text = Loc.s("calendar_reminderSchedule", "Reminder Schedule"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1.1f).height(58.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, null, tint = themeColor, modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val prefDateFormat = remember(context) { com.jtool.android.core.utils.getPreferredSimpleDateFormat(context) }
                                    val formattedDateStr = remember(reminderDate, prefDateFormat) {
                                        prefDateFormat.format(java.util.Date(reminderDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                                    }
                                    Text(Loc.s("calendar_reminderDate", "Date"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    Text(formattedDateStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }

                        Surface(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(0.9f).height(58.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, null, tint = themeColor, modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(Loc.s("calendar_reminderTime", "Time"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    Text(reminderTime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.alpha(0.1f))

                    val notificationLabel = when (reminderMinutesInput) {
                        0 -> Loc.s("calendar_atTimeOfReminder", "At time of reminder")
                        5 -> Loc.s("calendar_5mBefore", "5 minutes before")
                        10 -> Loc.s("calendar_10mBefore", "10 minutes before")
                        15 -> Loc.s("calendar_15mBefore", "15 minutes before")
                        30 -> Loc.s("calendar_30mBefore", "30 minutes before")
                        60 -> Loc.s("calendar_1hBefore", "1 hour before")
                        1440 -> Loc.s("calendar_1dBefore", "1 day before")
                        else -> "$reminderMinutesInput ${Loc.General.minuteSymbol} ${Loc.s("calendar_before", "before")}"
                    }

                    ActionListItem(
                        headline = Loc.s("calendar_notificationAlert", "Notification Alert"),
                        supportingText = notificationLabel,
                        leadingContent = { Icon(Icons.Default.NotificationsActive, null, tint = themeColor) },
                        trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                        onClick = { showNotificationLeadSheet = true }
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Loc.s("calendar_repeatReminder", "Repeat Reminder"),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { checked ->
                                isRecurring = checked
                                if (checked && rrule.isEmpty()) {
                                    rrule = "FREQ=DAILY"
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = themeColor,
                                checkedTrackColor = themeColor.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (isRecurring) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.1f))

                        val frequencyLabel = when {
                            rrule.contains("FREQ=DAILY") -> CalendarLoc.repeatEveryDay
                            rrule.contains("FREQ=WEEKLY") -> CalendarLoc.repeatEveryWeek
                            rrule.contains("FREQ=MONTHLY") -> CalendarLoc.repeatEveryMonth
                            rrule.contains("FREQ=YEARLY") -> CalendarLoc.repeatEveryYear
                            else -> CalendarLoc.repeatEveryDay
                        }

                        ActionListItem(
                            headline = CalendarLoc.repeatFrequency,
                            supportingText = frequencyLabel,
                            leadingContent = { Icon(Icons.Default.Repeat, null, tint = themeColor) },
                            trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                            onClick = { showRepeatFrequencySheet = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = Loc.s("calendar_reminderNote", "Reminder Note"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(Loc.s("calendar_reminderNote", "Reminder Note")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                trailingIcon = {
                    if (note.isNotBlank()) {
                        IconButton(onClick = { note = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
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
