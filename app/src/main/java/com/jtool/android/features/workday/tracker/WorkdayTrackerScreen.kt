package com.jtool.android.features.workday.tracker

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.DayLogHours
import com.jtool.android.core.models.workday.enums.PickerTypeEnum
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import kotlinx.coroutines.launch
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
    val timePickerState = rememberTimePickerState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.onEvent(WorkdayTrackerEvent.Refresh)
    }

    if (uiState.showDatePicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.DATE, show = false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Select Date", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
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
                ) { Text(Loc.general_save) }
            }
        }
    }

    if (uiState.showTimePicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WorkdayTrackerEvent.TogglePicker(picker = PickerTypeEnum.TIME, show = false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Select Time", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                ) { Text(Loc.general_select) }
            }
        }
    }

    if (uiState.showResetConfirm) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WorkdayTrackerEvent.ToggleResetConfirm(show = false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = Loc.worktime_resetConfirmTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(text = Loc.worktime_resetConfirmMsg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.onEvent(WorkdayTrackerEvent.ResetSession) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(Loc.general_delete) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.onEvent(WorkdayTrackerEvent.ToggleResetConfirm(show = false)) }, modifier = Modifier.fillMaxWidth()) { Text(Loc.general_cancel) }
            }
        }
    }

    if (uiState.showStartConfirm) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(WorkdayTrackerEvent.ToggleStartConfirm(false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = Loc.worktime_resetConfirmTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(text = "Starting this session will finish your current running session. Proceed?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { viewModel.onEvent(WorkdayTrackerEvent.ConfirmStartSession) }, modifier = Modifier.fillMaxWidth()) { Text(Loc.general_update) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.onEvent(WorkdayTrackerEvent.ToggleStartConfirm(false)) }, modifier = Modifier.fillMaxWidth()) { Text(Loc.general_cancel) }
            }
        }
    }

    val pref = uiState.trackingPreference
    val allowedModes = remember(pref) {
        when (pref) {
            WorkTrackingPreferenceEnum.LOG_HOURS -> listOf(WorkModeEnum.QUICK_START, WorkModeEnum.LOG_HOURS)
            WorkTrackingPreferenceEnum.SCHEDULE -> listOf(WorkModeEnum.QUICK_START, WorkModeEnum.SCHEDULE)
            else -> listOf(WorkModeEnum.QUICK_START)
        }
    }

    if (pref == null || pref == WorkTrackingPreferenceEnum.UNSET) {
        TrackingPreferenceBottomSheet(
            onDismiss = { },
            onPreferenceSelected = { viewModel.onEvent(WorkdayTrackerEvent.SetTrackingPreference(it)) }
        )
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 110.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        topBar = {
            GenericTitle(
                title = Loc.worktime_workTime,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onNavigate(AppScreenEnum.WORK_TIME_SETTINGS_MAIN) }) {
                        Icon(Icons.Default.Settings, null)
                    }
                }
            )
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Loc.worktime_shiftOptions,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (uiState.isSessionActive) {
                        TextButton(
                            onClick = { viewModel.onEvent(WorkdayTrackerEvent.ToggleResetConfirm(true)) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Loc.general_delete)
                        }
                    }
                }

                when (uiState.mode) {
                    WorkModeEnum.QUICK_START -> QuickStartContent(uiState = uiState, viewModel = viewModel)
                    WorkModeEnum.LOG_HOURS -> LogHoursContent(uiState = uiState, viewModel = viewModel, onNavigate = onNavigate)
                    WorkModeEnum.SCHEDULE -> ScheduleContent(uiState = uiState, viewModel = viewModel, onNavigate = onNavigate)
                }
                
                if (!uiState.isSessionActive) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.onEvent(WorkdayTrackerEvent.StartSession) },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(Loc.worktime_start)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (pref != null && pref != WorkTrackingPreferenceEnum.UNSET) {
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
                                    if (mode == WorkModeEnum.QUICK_START) {
                                        scope.launch { scaffoldState.bottomSheetState.expand() }
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = allowedModes.size)
                            ) {
                                Text(
                                    text = when(mode) {
                                        WorkModeEnum.QUICK_START -> Loc.worktime_freehand
                                        WorkModeEnum.LOG_HOURS -> Loc.worktime_manual
                                        WorkModeEnum.SCHEDULE -> Loc.worktime_weekly
                                    },
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
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
                
                val today = LocalDate.now().dayOfWeek
                val logConfig = uiState.logSchedule[today] ?: DayLogHours(isConfirmedOff = true)

                if (uiState.todayCalendarShift != null && !uiState.isSessionActive && uiState.mode != WorkModeEnum.QUICK_START) {
                    uiState.todayCalendarShift?.let { shift ->
                        Spacer(Modifier.height(24.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(Loc.calendar_shift, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "${shift.startTime} - ${shift.endTime ?: "??"}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (!uiState.isSessionActive) {
                    if (uiState.mode == WorkModeEnum.SCHEDULE) {
                        Spacer(Modifier.height(24.dp))
                        Text(Loc.worktime_dayOff, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    } else if (uiState.mode == WorkModeEnum.LOG_HOURS) {
                        if (logConfig.isConfirmedOff) {
                            Spacer(Modifier.height(24.dp))
                            Text(Loc.worktime_dayOff, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Spacer(Modifier.height(110.dp)) // Matching peek height
        }
    }
}

@Composable
fun RealTimeStatus(uiState: WorkdayTrackerUiState, viewModel: WorkdayTrackerViewModel, size: androidx.compose.ui.unit.Dp = 200.dp) {
    val color = if (uiState.isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val label = if (uiState.isFinished) Loc.worktime_overtime else Loc.worktime_timeLeft
    val time = if (uiState.isFinished) uiState.overtimeMillis else uiState.timeLeftMillis
    
    val showTimer = uiState.isSessionActive

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            CircularProgressIndicator(
                progress = { if (uiState.isFinished) 1f else uiState.progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = (size / 16),
                color = if (showTimer) color else color.copy(alpha = 0.2f),
                trackColor = color.copy(alpha = 0.1f)
            )
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (!showTimer) {
                    val today = LocalDate.now().dayOfWeek
                    val logConfig = uiState.logSchedule[today] ?: DayLogHours(isConfirmedOff = true)
                    
                    val isStartDisabled = when(uiState.mode) {
                        WorkModeEnum.SCHEDULE -> uiState.todayCalendarShift == null
                        WorkModeEnum.LOG_HOURS -> logConfig.isConfirmedOff
                        else -> false
                    }

                    Surface(
                        onClick = { if (!isStartDisabled) viewModel.onEvent(WorkdayTrackerEvent.StartSession) },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.size(size * 0.6f),
                        color = Color.Transparent,
                        enabled = !isStartDisabled
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.PlayArrow, 
                                null, 
                                modifier = Modifier.size(size * 0.24f),
                                tint = if (isStartDisabled) color.copy(alpha = 0.3f) else color
                            )
                            Text(
                                Loc.worktime_start, 
                                fontWeight = FontWeight.Bold,
                                fontSize = (size.value * 0.08f).sp,
                                color = if (isStartDisabled) color.copy(alpha = 0.3f) else color
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = formatMillis(time),
                            style = if (size > 200.dp) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Button(
                            onClick = { viewModel.onEvent(WorkdayTrackerEvent.FinishSession) },
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = color)
                        ) {
                            Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(Loc.worktime_finish, fontSize = 12.sp)
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
