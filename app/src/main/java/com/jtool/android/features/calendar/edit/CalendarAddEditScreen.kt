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
import com.jtool.android.features.calendar.edit.components.CalendarAddEditFormContent
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
    
    val selectedGroupIds = remember(stateKey) {
        val initialGroups = when {
            extension != null -> extension.sharedGroupIds
            event == null || event.id.isEmpty() -> {
                val activeGid = settingsManager.getCurrentGroupId()
                if (activeGid != null && groups[activeGid]?.isPrivate == false) listOf(activeGid) else emptyList()
            }
            else -> emptyList()
        }
        mutableStateListOf<String>().apply { addAll(initialGroups) }
    }

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
        title = if (event == null || event.id.isEmpty()) Loc.Calendar.addEvent else Loc.Calendar.editEvent,
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
                        dateTimestamp = dtStart,
                        endTimestamp = dtEnd ?: (dtStart + 3600_000L)
                    )

                    onConfirm(finalEvent, finalExt)
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = Loc.General.save)
            }
        }
    ) { _ ->
        CalendarAddEditFormContent(
            themeColor = themeColor,
            title = title,
            onTitleChange = { title = it },
            colorKey = colorKey,
            onShowColorPicker = { showColorPicker = true },
            startDate = startDate,
            onShowStartDatePicker = { showStartDatePicker = true },
            endDate = endDate,
            onShowEndDatePicker = { showEndDatePicker = true },
            startTime = startTime,
            onPickStartTime = { pickingStartTime = true },
            endTime = endTime,
            onPickEndTime = { pickingStartTime = false },
            allDay = allDay,
            onAllDayChange = { allDay = it },
            isRecurring = isRecurring,
            onIsRecurringChange = { checked ->
                isRecurring = checked
                if (checked && rrule.isEmpty()) {
                    rrule = "FREQ=DAILY"
                }
            },
            rrule = rrule,
            onShowRepeatFrequencySheet = { showRepeatFrequencySheet = true },
            isAdditionalExpanded = isAdditionalExpanded,
            onToggleAdditionalExpanded = { isAdditionalExpanded = !isAdditionalExpanded },
            locationInput = locationInput,
            onLocationChange = { locationInput = it },
            conferenceUrlInput = conferenceUrlInput,
            onConferenceUrlChange = { conferenceUrlInput = it },
            attachmentUrlInput = attachmentUrlInput,
            onAttachmentUrlChange = { attachmentUrlInput = it },
            selectedGroupIds = selectedGroupIds,
            groups = groups,
            onShowGroupPicker = { showGroupPicker = true },
            note = note,
            onNoteChange = { note = it }
        )
    }
}
