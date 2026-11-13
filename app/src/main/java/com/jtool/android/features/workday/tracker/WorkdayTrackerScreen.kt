package com.jtool.android.features.workday.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.utils.rememberCustomDatePickerState
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getEventColor
import com.jtool.android.features.workday.tracker.components.today_events_sheet.TodayEventsSheet
import java.time.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkdayTrackerScreen(
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: WorkdayTrackerViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                WorkdayTrackerViewModel(settingsManager, dataManager)
            }
        }
    )
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val datePickerState = rememberCustomDatePickerState()
    val timePickerState = rememberTimePickerState(is24Hour = true)

    LaunchedEffect(Unit) {
        viewModel.onEvent(WorkdayTrackerEvent.Refresh)
    }

    if (uiState.showQuickStartConfigError) {
        com.jtool.android.ui.components.common.MessageSheet(
            message = Loc.Calendar.selectCalendarSubtitle,
            type = com.jtool.android.ui.components.common.MessageType.ERROR,
            onDismiss = { viewModel.onEvent(WorkdayTrackerEvent.ToggleQuickStartError(false)) }
        )
    }

    if (uiState.showDatePicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.DATE, show = false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = Loc.Workday.selectDate, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                DatePicker(state = datePickerState)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            val current = if (uiState.pickingStartTime) uiState.freehandStart else uiState.freehandEnd
                            val safeCurrent = current ?: LocalDateTime.now()
                            val new = LocalDateTime.of(date, safeCurrent.toLocalTime())
                            if (uiState.pickingStartTime) viewModel.onEvent(WorkdayTrackerEvent.SetFreehandStart(new))
                            else viewModel.onEvent(WorkdayTrackerEvent.SetFreehandEnd(new))
                        }
                        viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.DATE, show = false))
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) { Text(Loc.General.save) }
            }
        }
    }

    if (uiState.showTimePicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.TIME, show = false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = Loc.Workday.selectTime, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                TimePicker(state = timePickerState)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        if (uiState.isPickingWeeklyToday) {
                            if (uiState.pickingStartTime) viewModel.onEvent(WorkdayTrackerEvent.SetWeeklyTodayStart(time))
                            else viewModel.onEvent(WorkdayTrackerEvent.SetWeeklyTodayEnd(time))
                        } else {
                            val current = if (uiState.pickingStartTime) uiState.freehandStart else uiState.freehandEnd
                            val safeCurrent = current ?: LocalDateTime.now()
                            val new = LocalDateTime.of(safeCurrent.toLocalDate(), time)
                            if (uiState.pickingStartTime) viewModel.onEvent(WorkdayTrackerEvent.SetFreehandStart(new))
                            else viewModel.onEvent(WorkdayTrackerEvent.SetFreehandEnd(new))
                        }
                        viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.TIME, show = false))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(Loc.General.select) }
            }
        }
    }

    if (uiState.showResetConfirm) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WorkdayTrackerEvent.ToggleResetConfirm(show = false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = Loc.Workday.resetConfirmTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(text = Loc.Workday.resetConfirmMsg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.onEvent(WorkdayTrackerEvent.ResetSession) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(Loc.General.reset) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.onEvent(WorkdayTrackerEvent.ToggleResetConfirm(show = false)) }, modifier = Modifier.fillMaxWidth()) { Text(Loc.General.cancel) }
            }
        }
    }

    if (uiState.showStartConfirm) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WorkdayTrackerEvent.ToggleStartConfirm(false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = Loc.Workday.resetConfirmTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(text = Loc.Workday.finishCurrentSessionConfirm, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { viewModel.onEvent(WorkdayTrackerEvent.ConfirmStartSession) }, modifier = Modifier.fillMaxWidth()) { Text(Loc.General.edit) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.onEvent(WorkdayTrackerEvent.ToggleStartConfirm(false)) }, modifier = Modifier.fillMaxWidth()) { Text(Loc.General.cancel) }
            }
        }
    }

    val allowedModes = listOf(WorkModeEnum.CALENDAR_EVENT, WorkModeEnum.QUICK_START)

    BaseScreen(
        title = Loc.Workday.workTime,
        isLoading = uiState.isLoading,
        onBack = onBack,
        actions = {
            com.jtool.android.core.components.layout.TitleDropdownMenu {
                com.jtool.android.core.components.layout.ActionListItem(
                    headline = Loc.Launcher.settings,
                    supportingText = Loc.s("workday_settingsSubtitle", "Configure start day, hours, schedule, and shift settings"),
                    leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { onNavigate(AppScreenEnum.WORK_TIME_SETTINGS_MAIN) }
                )
                com.jtool.android.core.components.layout.ActionListItem(
                    headline = Loc.General.learnMore,
                    supportingText = Loc.s("workday_learnMoreSubtitle", "View online guide and feature documentation"),
                    leadingContent = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "work-tracker") }
                )
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    allowedModes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = uiState.mode == mode,
                            onClick = { 
                                viewModel.onEvent(WorkdayTrackerEvent.ChangeMode(mode))
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = allowedModes.size),
                            icon = {
                                Icon(
                                    imageVector = if (mode == WorkModeEnum.QUICK_START) Icons.Default.Bolt else Icons.Default.Work,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(
                                text = when(mode) {
                                    WorkModeEnum.CALENDAR_EVENT -> Loc.Calendar.event
                                    WorkModeEnum.QUICK_START -> Loc.Workday.freehand
                                    else -> ""
                                },
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (uiState.mode == WorkModeEnum.CALENDAR_EVENT) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (uiState.todayEvents.isNotEmpty()) {
                        EventPickerSection(uiState, viewModel, settingsManager, onNavigate)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Loc.Workday.noWorkDetected, 
                                color = Color.Gray, 
                                fontWeight = FontWeight.Bold, 
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RealTimeStatus(uiState, viewModel, 240.dp)
                }
            } else {
                val plannedEnd = remember(
                    uiState.freehandStart,
                    uiState.freehandEnd,
                    uiState.freehandDurationHours,
                    uiState.freehandDurationMinutes,
                    uiState.isUsingDuration
                ) {
                    val startDt = uiState.freehandStart ?: LocalDateTime.now()
                    if (uiState.isUsingDuration) {
                        startDt.plusHours(uiState.freehandDurationHours.toLong())
                            .plusMinutes(uiState.freehandDurationMinutes.toLong())
                    } else {
                        uiState.freehandEnd ?: startDt.plusHours(8)
                    }
                }
                val timeFmt = remember { java.time.format.DateTimeFormatter.ofPattern("HH:mm") }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Loc.Workday.shiftOptions,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${Loc.Workday.finishAt}: ${plannedEnd.format(timeFmt)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            QuickStartContent(uiState = uiState, viewModel = viewModel)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = { viewModel.onEvent(WorkdayTrackerEvent.StartSession) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(Loc.Workday.start, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPickerSection(
    uiState: WorkdayTrackerUiState, 
    viewModel: WorkdayTrackerViewModel,
    settingsManager: SettingsManager,
    onNavigate: (AppScreenEnum) -> Unit
) {
    val selectedEvent = uiState.todayEvents.find { it.id == uiState.selectedEventId }
    var showPickerSheet by remember { mutableStateOf(false) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), MaterialTheme.shapes.medium)
                    .clickable { showPickerSheet = true }
                    .padding(horizontal = 16.dp)
            ) {
                val eventColor = getEventColor(selectedEvent?.colorKey)
                val workShiftColorKey = remember { settingsManager.getWorkShiftColorKey() }
                val isWork = selectedEvent != null && ((selectedEvent.colorKey != null && selectedEvent.colorKey == workShiftColorKey) || (selectedEvent.colorKey == null && (workShiftColorKey == "7" || workShiftColorKey == "1")))
                val nowMs = System.currentTimeMillis()
                val isEnded = selectedEvent != null && (selectedEvent.dtEnd ?: 0L) > 0L && nowMs > (selectedEvent.dtEnd ?: 0L)

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(eventColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWork) Icons.Default.Work else Icons.Default.Event,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedEvent?.title ?: "---",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (isEnded) {
                        Text(
                            text = Loc.Workday.eventEnded,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Icon(Icons.Default.ArrowDropDown, null)
            }

            if (selectedEvent != null) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        settingsManager.setSelectedCalendarEventId(selectedEvent.id)
                        viewModel.onEvent(WorkdayTrackerEvent.SelectEvent(selectedEvent.id))
                        onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                ) {
                    Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }

    if (showPickerSheet) {
        TodayEventsSheet(
            events = uiState.todayEvents,
            selectedEventId = uiState.selectedEventId,
            onSelectEvent = { event ->
                viewModel.onEvent(WorkdayTrackerEvent.SelectEvent(event.id))
            },
            onDismiss = { showPickerSheet = false },
            workShiftColorKey = settingsManager.getWorkShiftColorKey()
        )
    }
}
