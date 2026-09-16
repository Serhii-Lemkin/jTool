package com.jtool.android.features.workday.tracker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.DayLogHours
import com.jtool.android.core.models.workday.DaySchedule
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import com.jtool.android.features.calendar.common.CalendarEventHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun QuickStartContent(uiState: WorkdayTrackerUiState, viewModel: WorkdayTrackerViewModel) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DateTimeSelector(
            label = Loc.Workday.start,
            dateTime = uiState.freehandStart,
            onDateClick = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.DATE, show = true, isStart = true)) },
            onTimeClick = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.TIME, show = true, isStart = true)) }
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !uiState.isUsingDuration,
                onClick = { viewModel.onEvent(WorkdayTrackerEvent.SetUseDuration(use = false)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(Loc.Workday.setEndTime, fontSize = 12.sp) }
            SegmentedButton(
                selected = uiState.isUsingDuration,
                onClick = { viewModel.onEvent(WorkdayTrackerEvent.SetUseDuration(use = true)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(Loc.Workday.setDuration, fontSize = 12.sp) }
        }
        
        if (uiState.isUsingDuration) {
            PreciseDurationPicker(
                hours = uiState.freehandDurationHours,
                minutes = uiState.freehandDurationMinutes,
                onHoursChange = { h -> viewModel.onEvent(WorkdayTrackerEvent.SetFreehandDuration(hours = h, minutes = uiState.freehandDurationMinutes)) },
                onMinutesChange = { m -> viewModel.onEvent(WorkdayTrackerEvent.SetFreehandDuration(hours = uiState.freehandDurationHours, minutes = m)) }
            )
        } else {
            DateTimeSelector(
                label = Loc.Workday.finish,
                dateTime = uiState.freehandEnd,
                onDateClick = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.DATE, show = true, isStart = false)) },
                onTimeClick = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.TIME, show = true, isStart = false)) }
            )
        }
    }
}

@Composable
fun LogHoursContent(uiState: WorkdayTrackerUiState, viewModel: WorkdayTrackerViewModel, onNavigate: (AppScreenEnum) -> Unit) {
    val anySchedule = uiState.logSchedule.isNotEmpty()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!anySchedule) {
                Text(
                    Loc.Workday.noHoursScheduled,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { onNavigate(AppScreenEnum.WORK_TIME_LOG_CONFIG) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.Workday.setWorkHours)
                }
            } else {
                val today = java.time.LocalDate.now().dayOfWeek
                val config = uiState.logSchedule[today] ?: DayLogHours(isConfirmedOff = true)

                if (config.isConfirmedOff) {
                    Text(
                        Loc.Workday.dayOff,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { onNavigate(AppScreenEnum.WORK_TIME_LOG_CONFIG) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Settings, null)
                        Spacer(Modifier.width(8.dp))
                        Text(Loc.Workday.setWorkHours)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.Workday.start, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }

                    Text(
                        "${Loc.Workday.duration}: ${config.hours}${Loc.General.hourSymbol} ${config.minutes}${Loc.General.minuteSymbol}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    val startTime = uiState.freehandStart ?: LocalDateTime.now()
                    
                    OutlinedButton(
                        onClick = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.TIME, show = true, isStart = true)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccessTime, null)
                        Spacer(Modifier.width(8.dp))
                        Text(startTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleContent(uiState: WorkdayTrackerUiState, viewModel: WorkdayTrackerViewModel, onNavigate: (AppScreenEnum) -> Unit) {
    val shift = uiState.todayCalendarShift

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (shift == null) {
                Text(
                    Loc.Workday.noWorkDetected,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { onNavigate(AppScreenEnum.WORK_TIME_SCHEDULE) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.Workday.setSchedule)
                }
            } else {
                Column {
                    Text(Loc.Workday.workTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val now = java.time.LocalTime.now()
                    val end = shift.endTime?.let { CalendarEventHelper.parseTime(it) }
                    val isFinished = end != null && now.isAfter(end)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Today's Shift: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Icon(
                            Icons.Default.AccessTime, 
                            null, 
                            modifier = Modifier.size(16.dp), 
                            tint = if (isFinished) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${shift.startTime} - ${shift.endTime ?: "--:--"}", 
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isFinished) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { 
                            onNavigate(AppScreenEnum.WORK_TIME_SCHEDULE)
                        }) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreciseDurationPicker(hours: Int, minutes: Int, onHoursChange: (Int) -> Unit, onMinutesChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = if (hours == 0) "" else hours.toString(),
            onValueChange = { 
                val v = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                if (v < 24) onHoursChange(v)
            },
            label = { Text(Loc.General.hours) },
            modifier = Modifier.weight(1f),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            singleLine = true
        )
        OutlinedTextField(
            value = if (minutes == 0) "" else minutes.toString(),
            onValueChange = { 
                val v = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                if (v < 60) onMinutesChange(v)
            },
            label = { Text(Loc.General.minutes) },
            modifier = Modifier.weight(1f),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            singleLine = true
        )
    }
}

@Composable
fun CommonSessionControls(uiState: WorkdayTrackerUiState, viewModel: WorkdayTrackerViewModel) {
    val isSessionInCurrentMode = uiState.isSessionActive && uiState.activeSessionMode == uiState.mode

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isSessionInCurrentMode) {
            Button(
                onClick = { viewModel.onEvent(WorkdayTrackerEvent.FinishSession) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Done, null)
                Spacer(Modifier.width(8.dp))
                Text(Loc.Workday.finishSession)
            }
            
            OutlinedButton(
                onClick = { viewModel.onEvent(WorkdayTrackerEvent.ToggleResetConfirm(show = true)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(Loc.General.delete)
            }
        } else {
            Button(
                onClick = { viewModel.onEvent(WorkdayTrackerEvent.StartSession) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(Loc.Workday.start)
            }
        }
    }
}

@Composable
fun DateTimeSelector(label: String, dateTime: LocalDateTime?, onDateClick: () -> Unit, onTimeClick: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDateClick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(dateTime?.format(DateTimeFormatter.ofPattern("dd.MM")) ?: "--.--")
            }
            OutlinedButton(onClick = onTimeClick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingPreferenceBottomSheet(
    onDismiss: () -> Unit,
    onPreferenceSelected: (WorkTrackingPreferenceEnum) -> Unit,
    selectedPreference: WorkTrackingPreferenceEnum? = null
) {
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
                text = Loc.Workday.choosePreferenceTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Loc.Workday.choosePreferenceMsg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))

            PreferenceOption(
                title = Loc.Workday.manual,
                description = Loc.Workday.goalsExpl,
                icon = Icons.Default.Timer,
                onClick = { onPreferenceSelected(WorkTrackingPreferenceEnum.LOG_HOURS) },
                trailingContent = if (selectedPreference == WorkTrackingPreferenceEnum.LOG_HOURS) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            PreferenceOption(
                title = Loc.Workday.weekly,
                description = Loc.Workday.scheduleExpl,
                icon = Icons.Default.DateRange,
                onClick = { onPreferenceSelected(WorkTrackingPreferenceEnum.SCHEDULE) },
                trailingContent = if (selectedPreference == WorkTrackingPreferenceEnum.SCHEDULE) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            PreferenceOption(
                title = Loc.General.selectLater,
                description = Loc.General.selectLaterExpl,
                icon = Icons.Default.MoreTime,
                onClick = { onPreferenceSelected(WorkTrackingPreferenceEnum.UNSET) },
                trailingContent = if (selectedPreference == WorkTrackingPreferenceEnum.UNSET) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null
            )
        }
    }
}

@Composable
fun PreferenceOption(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ActionListItem(
        headline = title,
        supportingText = description,
        supportingMaxLines = Int.MAX_VALUE, // Do not truncate description
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = trailingContent,
        onClick = onClick
    )
}
