package com.jtool.android.features.workday.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
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
    val uiState by viewModel.uiState.collectAsState()
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(is24Hour = true)
    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(WorkdayTrackerEvent.Refresh)
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

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (uiState.mode == WorkModeEnum.QUICK_START && !uiState.isSessionActive) 110.dp else 0.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = { if (uiState.mode == WorkModeEnum.QUICK_START && !uiState.isSessionActive) BottomSheetDefaults.DragHandle() else null },
        topBar = {
            GenericTitle(
                title = Loc.Workday.workTime,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onNavigate(AppScreenEnum.WORK_TIME_SETTINGS_MAIN) }) {
                        Icon(Icons.Default.Settings, null)
                    }
                }
            )
        },
        sheetContent = {
            if (uiState.mode == WorkModeEnum.QUICK_START && !uiState.isSessionActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = Loc.Workday.shiftOptions,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    
                    QuickStartContent(uiState = uiState, viewModel = viewModel)
                    
                    Spacer(Modifier.height(16.dp))
                    CommonSessionControls(uiState = uiState, viewModel = viewModel)
                }
            } else {
                Box(Modifier.height(1.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
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
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = allowedModes.size)
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val timerSize = 260.dp
                Box(contentAlignment = Alignment.Center) {
                    RealTimeStatus(uiState, viewModel, timerSize)
                }
                
                Spacer(Modifier.height(32.dp))

                if (uiState.mode == WorkModeEnum.CALENDAR_EVENT) {
                    if (uiState.todayEvents.isNotEmpty() && !uiState.isSessionActive) {
                        EventPickerSection(uiState, viewModel, onNavigate)
                    } else if (!uiState.isSessionActive) {
                        Text(Loc.Workday.dayOff, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(Modifier.height(if (uiState.mode == WorkModeEnum.QUICK_START && !uiState.isSessionActive) 110.dp else 0.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPickerSection(
    uiState: WorkdayTrackerUiState, 
    viewModel: WorkdayTrackerViewModel,
    onNavigate: (AppScreenEnum) -> Unit
) {
    val selectedEvent = uiState.todayEvents.find { it.id == uiState.selectedEventId }
    var showPickerSheet by remember { mutableStateOf(false) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(Loc.Workday.selectEvent, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        
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
                Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = selectedEvent?.title ?: "---",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(Icons.Default.ArrowDropDown, null)
            }

            if (selectedEvent != null) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.onEvent(WorkdayTrackerEvent.SelectEvent(selectedEvent.id))
                        onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }

    if (showPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = Loc.Workday.selectEvent,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn {
                    items(uiState.todayEvents) { event ->
                        val isSelected = uiState.selectedEventId == event.id
                        val typeText = if (event.type == CalendarEventTypeEnum.SHIFT) Loc.Calendar.shift else Loc.Calendar.common
                        
                        ListItem(
                            headlineContent = { Text(event.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = { Text("${event.startTime} - ${event.endTime ?: Loc.General.none} ($typeText)") },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                viewModel.onEvent(WorkdayTrackerEvent.SelectEvent(event.id))
                                showPickerSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RealTimeStatus(uiState: WorkdayTrackerUiState, viewModel: WorkdayTrackerViewModel, size: Dp = 200.dp) {
    val currentTimer = if (uiState.mode == WorkModeEnum.QUICK_START) uiState.quickStartTimer else uiState.eventTimer
    
    val color = if (currentTimer.isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val label = if (currentTimer.isFinished) Loc.Workday.overtime else (currentTimer.label ?: Loc.Workday.timeLeft)
    val time = if (currentTimer.isFinished) currentTimer.overtimeMillis else currentTimer.timeLeftMillis
    
    val showTimer = if (uiState.mode == WorkModeEnum.QUICK_START) {
        uiState.isSessionActive && uiState.activeSessionMode == WorkModeEnum.QUICK_START
    } else {
        uiState.selectedEventId != null
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            CircularProgressIndicator(
                progress = { if (currentTimer.isFinished) 1f else currentTimer.progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = (size / 16),
                color = if (showTimer) color else color.copy(alpha = 0.2f),
                trackColor = color.copy(alpha = 0.1f)
            )
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val isCountdownToStart = uiState.mode == WorkModeEnum.CALENDAR_EVENT && currentTimer.label?.contains(Loc.Workday.untilStart) == true
                
                if (!showTimer || (uiState.mode == WorkModeEnum.CALENDAR_EVENT && isCountdownToStart)) {
                    if (uiState.mode == WorkModeEnum.QUICK_START) {
                        Surface(
                            onClick = { viewModel.onEvent(WorkdayTrackerEvent.StartSession) },
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.size(size * 0.6f),
                            color = Color.Transparent
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(size * 0.24f), tint = color)
                                Text(Loc.Workday.start, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.08f).sp, color = color)
                            }
                        }
                    } else if (uiState.mode == WorkModeEnum.CALENDAR_EVENT && uiState.selectedEventId != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = formatMillis(time),
                                style = if (size > 200.dp) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = formatMillis(time),
                            style = if (size > 200.dp) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )

                        val isCalendarFinish = uiState.mode == WorkModeEnum.CALENDAR_EVENT && currentTimer.label?.contains(Loc.Workday.finishAt) == true
                        val canFinish = (uiState.mode == WorkModeEnum.QUICK_START && uiState.isSessionActive) || isCalendarFinish

                        if (canFinish) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.onEvent(WorkdayTrackerEvent.FinishSession) },
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = color)
                            ) {
                                Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(Loc.Workday.finish, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val h = millis / 3600000
    val m = (millis % 3600000) / 60000
    val s = (millis % 60000) / 1000
    return String.format(Locale.ENGLISH, "%02d:%02d:%02d", h, m, s)
}
