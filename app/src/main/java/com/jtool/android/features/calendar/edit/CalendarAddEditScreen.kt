package com.jtool.android.features.calendar.edit

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.utils.rememberCustomDatePickerState
import com.jtool.android.features.calendar.components.group_picker_sheet.GroupPickerSheet
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.EventColorPickerSheet
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getGoogleCalendarColorPalette
import com.jtool.android.features.calendar.edit.components.required_fields_sheet.RequiredFieldsSheet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
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
    val context = LocalContext.current
    val stateKey = remember(event?.id, selectedDate) { Any() }

    var title by remember(stateKey) { mutableStateOf(event?.title ?: "") }
    var note by remember(stateKey) { mutableStateOf(event?.note ?: "") }
    var startTime by remember(stateKey) { mutableStateOf(if (event?.startTime.isNullOrBlank()) "09:00" else event!!.startTime) }
    var endTime by remember(stateKey) { mutableStateOf(event?.endTime ?: "") }
    var allDay by remember(stateKey) { mutableStateOf(event?.allDay ?: false) }
    var startDate by remember(stateKey) { 
        mutableStateOf(if (event?.date != null && event.date.isNotEmpty()) LocalDate.parse(event.date) else selectedDate) 
    }
    var endDate by remember(stateKey) {
        mutableStateOf(
            if (event?.dtEnd != null) {
                if (event.allDay) Instant.ofEpochMilli(event.dtEnd).atZone(ZoneOffset.UTC).toLocalDate()
                else Instant.ofEpochMilli(event.dtEnd).atZone(ZoneId.systemDefault()).toLocalDate()
            } else startDate
        )
    }
    
    var colorKey by remember(stateKey) { mutableStateOf(event?.colorKey ?: "7") }
    var isRecurring by remember(stateKey) { mutableStateOf(!event?.rrule.isNullOrEmpty()) }
    var rrule by remember(stateKey) { mutableStateOf(event?.rrule ?: "") }

    var isAdditionalExpanded by remember(stateKey) { mutableStateOf(!event?.location.isNullOrEmpty() || !event?.conferenceUrl.isNullOrEmpty() || !event?.attachmentUrl.isNullOrEmpty()) }
    var locationInput by remember(stateKey) { mutableStateOf(event?.location ?: "") }
    var conferenceUrlInput by remember(stateKey) { mutableStateOf(event?.conferenceUrl ?: "") }
    var attachmentUrlInput by remember(stateKey) { mutableStateOf(event?.attachmentUrl ?: "") }
    var reminderMinutesInput by remember(stateKey) { mutableStateOf(event?.reminderMinutes ?: 15) }
    
    val selectedGroupIds = remember(stateKey) { mutableStateListOf<String>().apply { addAll(extension?.sharedGroupIds ?: emptyList()) } }

    var showGroupPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showRepeatFrequencySheet by remember { mutableStateOf(false) }
    var showRequiredFieldsSheet by remember { mutableStateOf(false) }
    var pickingStartTime by remember { mutableStateOf<Boolean?>(null) }

    val themeColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(startTime, endTime) {
        if (!allDay && startTime.isNotBlank() && endTime.isNotBlank()) {
            try {
                val startT = LocalTime.parse(startTime)
                val endT = LocalTime.parse(endTime)
                if (endT.isBefore(startT) && endDate == startDate) {
                    endDate = startDate.plusDays(1)
                }
            } catch (_: Exception) {}
        }
    }

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

    if (showStartDatePicker) {
        val datePickerState = rememberCustomDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val picked = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        startDate = picked
                        if (endDate.isBefore(picked)) endDate = picked
                    }
                    showStartDatePicker = false
                }) { Text(Loc.General.select) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text(Loc.General.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberCustomDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val picked = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        endDate = picked
                    }
                    showEndDatePicker = false
                }) { Text(Loc.General.select) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text(Loc.General.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
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
                    text = com.jtool.android.core.localization.domains.CalendarLoc.repeatFrequency,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val options = listOf(
                    "FREQ=DAILY" to com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryDay,
                    "FREQ=WEEKLY" to com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryWeek,
                    "FREQ=MONTHLY" to com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryMonth,
                    "FREQ=YEARLY" to com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryYear
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

    BaseScreen(
        title = if (event == null) Loc.Calendar.addEvent else Loc.Calendar.editEvent,
        onBack = onBack,
        actions = {
            IconButton(
                onClick = {
                    if (title.trim().isEmpty()) {
                        showRequiredFieldsSheet = true
                        return@IconButton
                    }

                    val effectiveStartStr = if (startTime.isBlank()) "09:00" else startTime

                    val (dtStart, dtEnd) = if (allDay) {
                        val startUtc = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                        val endUtc = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                        startUtc to endUtc
                    } else {
                        val startLdt = startDate.atTime(LocalTime.parse(effectiveStartStr))
                        val endLdt = if (endTime.isNotBlank()) {
                            endDate.atTime(LocalTime.parse(endTime))
                        } else {
                            startLdt.plusMinutes(15)
                        }
                        val startMs = startLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val endMs = endLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        startMs to endMs
                    }

                    if (dtEnd <= dtStart) {
                        showRequiredFieldsSheet = true
                        return@IconButton
                    }

                    val effectiveRrule = if (isRecurring) (if (rrule.isEmpty()) "FREQ=DAILY" else rrule) else null
                    val computedDuration = if (isRecurring) {
                        com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper.calculateRfcDuration(dtStart, dtEnd, allDay)
                    } else null

                    val finalEvent = CalendarEvent(
                        id = event?.id ?: "",
                        title = title.trim(),
                        allDay = allDay,
                        note = note,
                        dtStart = dtStart,
                        dtEnd = if (isRecurring) null else dtEnd,
                        colorKey = colorKey,
                        rrule = effectiveRrule,
                        duration = computedDuration,
                        location = locationInput.trim().ifEmpty { null },
                        conferenceUrl = conferenceUrlInput.trim().ifEmpty { null },
                        attachmentUrl = attachmentUrlInput.trim().ifEmpty { null },
                        reminderMinutes = reminderMinutesInput,
                        googleCalendarId = event?.googleCalendarId ?: settingsManager.getGoogleCalendarId()
                    )
                    
                    val finalExt = CalendarEventExtension(
                        googleEventId = event?.id ?: "",
                        sharedGroupIds = selectedGroupIds.toList(),
                        dateTimestamp = dtStart
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
                                Text(Loc.Calendar.eventTitle)
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
                        CompactPickerButton(
                            label = Loc.s("calendar_startDate", "Start Date"),
                            value = startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                            icon = Icons.Default.CalendarToday,
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1.1f)
                        )
                        if (!allDay) {
                            CompactPickerButton(
                                label = Loc.Workday.start,
                                value = startTime,
                                icon = Icons.Default.AccessTime,
                                onClick = { pickingStartTime = true },
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.alpha(0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactPickerButton(
                            label = Loc.s("calendar_endDate", "End Date"),
                            value = endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                            icon = Icons.Default.Event,
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1.1f)
                        )
                        if (!allDay) {
                            CompactPickerButton(
                                label = Loc.Workday.finish,
                                value = if (endTime.isEmpty()) Loc.General.none else endTime,
                                icon = Icons.Default.AccessTime,
                                onClick = { pickingStartTime = false },
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.alpha(0.1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.Calendar.allDay, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(
                            checked = allDay,
                            onCheckedChange = { allDay = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = themeColor, checkedTrackColor = themeColor.copy(alpha = 0.5f))
                        )
                    }
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
                            text = com.jtool.android.core.localization.domains.CalendarLoc.repeatEvent,
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
                            rrule.contains("FREQ=DAILY") -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryDay
                            rrule.contains("FREQ=WEEKLY") -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryWeek
                            rrule.contains("FREQ=MONTHLY") -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryMonth
                            rrule.contains("FREQ=YEARLY") -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryYear
                            else -> com.jtool.android.core.localization.domains.CalendarLoc.repeatEveryDay
                        }

                        ActionListItem(
                            headline = com.jtool.android.core.localization.domains.CalendarLoc.repeatFrequency,
                            supportingText = frequencyLabel,
                            leadingContent = { Icon(Icons.Default.Repeat, null, tint = themeColor) },
                            trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                            onClick = { showRepeatFrequencySheet = true }
                        )
                    }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdditionalExpanded = !isAdditionalExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tune, null, tint = themeColor)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = Loc.s("calendar_additionalOptions", "Additional Options"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isAdditionalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    if (isAdditionalExpanded) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.1f))

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = locationInput,
                                    onValueChange = { locationInput = it },
                                    label = { Text(Loc.s("calendar_eventLocation", "Location / Address")) },
                                    leadingIcon = { Icon(Icons.Default.Place, null, tint = themeColor) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                                    trailingIcon = {
                                        if (locationInput.isNotBlank()) {
                                            IconButton(onClick = { locationInput = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                if (locationInput.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(locationInput)}")
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            val chooser = Intent.createChooser(intent, Loc.s("calendar_openLocationWith", "Open Location With"))
                                            try {
                                                context.startActivity(chooser)
                                            } catch (_: Exception) {
                                                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(locationInput)}")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Map, null, tint = themeColor)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = conferenceUrlInput,
                                onValueChange = { conferenceUrlInput = it },
                                label = { Text(Loc.s("calendar_conferenceUrl", "Video Meeting Link (Google Meet / Zoom)")) },
                                leadingIcon = { Icon(Icons.Default.VideoCall, null, tint = themeColor) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    if (conferenceUrlInput.isNotBlank()) {
                                        IconButton(onClick = { conferenceUrlInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            )

                            OutlinedTextField(
                                value = attachmentUrlInput,
                                onValueChange = { attachmentUrlInput = it },
                                label = { Text(Loc.s("calendar_attachmentUrl", "Attachment / Document Link")) },
                                leadingIcon = { Icon(Icons.Default.AttachFile, null, tint = themeColor) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    if (attachmentUrlInput.isNotBlank()) {
                                        IconButton(onClick = { attachmentUrlInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
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

@Composable
private fun CompactPickerButton(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
