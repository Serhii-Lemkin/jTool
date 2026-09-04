package com.jtool.android.features.workday.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.jtool.android.core.models.workday.DaySchedule
import com.jtool.android.core.models.workday.WorkShift
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSettingsScreen(
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ScheduleViewModel(settingsManager, dataManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val timePickerState = rememberTimePickerState()

    LaunchedEffect(uiState.scrollToShiftKey) {
        uiState.scrollToShiftKey?.let { _ ->
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(ScheduleEvent.ClearScroll)
        }
    }

    val handleBack = {
        if (uiState.hasUnsavedChanges) {
            viewModel.onEvent(ScheduleEvent.ToggleExitConfirm(true))
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    // Automatically go back after successful save
    var wasSaving by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) wasSaving = true
        if (wasSaving && !uiState.isLoading && uiState.conflictError == null) {
            onBack()
        }
    }

    if (uiState.showExitConfirm) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(ScheduleEvent.ToggleExitConfirm(false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = Loc.Workday.unsavedChangesTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Loc.Workday.unsavedChangesMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Loc.Workday.exit)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.onEvent(ScheduleEvent.ToggleExitConfirm(false)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Loc.Workday.keepEditing)
                }
            }
        }
    }

    if (uiState.showTimePicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(ScheduleEvent.ToggleTimePicker(false)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Loc.General.select,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TimePicker(state = timePickerState)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.onEvent(ScheduleEvent.SetTime(LocalTime.of(timePickerState.hour, timePickerState.minute)))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Loc.General.select)
                }
            }
        }
    }

    if (uiState.conflictError != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(ScheduleEvent.ClearError) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = Loc.Workday.schedulingConflict,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.conflictError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.onEvent(ScheduleEvent.ClearError) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Loc.General.close)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            GenericTitle(
                title = Loc.Workday.editWorkHours,
                onBack = handleBack,
                isLoading = uiState.isLoading,
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(ScheduleEvent.Save) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Save, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Week Navigation
            val weekEnd = uiState.selectedWeekStart.plusDays(6)
            val monthFormatter = DateTimeFormatter.ofPattern("MMM")
            val dayFormatter = DateTimeFormatter.ofPattern("d")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onEvent(ScheduleEvent.NavigateWeek(-1)) }) {
                    Icon(Icons.Default.ChevronLeft, null)
                }
                
                Text(
                    text = "${uiState.selectedWeekStart.format(dayFormatter)} ${uiState.selectedWeekStart.format(monthFormatter)} - ${weekEnd.format(dayFormatter)} ${weekEnd.format(monthFormatter)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                
                IconButton(onClick = { viewModel.onEvent(ScheduleEvent.NavigateWeek(1)) }) {
                    Icon(Icons.Default.ChevronRight, null)
                }
            }

            val isScheduleEmpty = uiState.tempWeeklySchedule.values.all { it.shifts.isEmpty() && !it.isConfirmedOff }

            if (isScheduleEmpty && uiState.previousWeeklySchedule.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.onEvent(ScheduleEvent.CopyPrevWeek) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.Workday.copyFromPrevWeek)
                }
            }

            Spacer(Modifier.height(16.dp))

            val sortedDays = remember(uiState.weeklyStartDay) {
                val all = DayOfWeek.entries
                val startIndex = all.indexOf(uiState.weeklyStartDay)
                all.subList(startIndex, all.size) + all.subList(0, startIndex)
            }

            val weekStartDate = uiState.selectedWeekStart

            sortedDays.forEachIndexed { dayIdx, day ->
                val dayOffset = (day.value - uiState.weeklyStartDay.value + 7) % 7
                val calendarDate = weekStartDate.plusDays(dayOffset.toLong())
                val daySchedule = uiState.tempWeeklySchedule[day] ?: DaySchedule()
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Loc.getDayOfWeekName(day),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (daySchedule.isConfirmedOff) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = calendarDate.format(DateTimeFormatter.ofPattern("dd MMM")),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        IconButton(onClick = { 
                            viewModel.onEvent(ScheduleEvent.UpdateTempSchedule(day, daySchedule.copy(isConfirmedOff = !daySchedule.isConfirmedOff, shifts = if (!daySchedule.isConfirmedOff) emptyList() else daySchedule.shifts)))
                        }) {
                            Icon(
                                if (daySchedule.isConfirmedOff) Icons.Default.EventBusy else Icons.Default.EventAvailable,
                                null,
                                tint = if (daySchedule.isConfirmedOff) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.onEvent(ScheduleEvent.AddShift(day)) },
                            enabled = !daySchedule.isConfirmedOff
                        ) {
                            Icon(Icons.Default.Add, null)
                        }
                    }

                    if (!daySchedule.isConfirmedOff) {
                        daySchedule.shifts.forEachIndexed { index, shift ->
                            val shiftKey = "${day.name}_$index"
                            val isConflicting = uiState.conflictingShifts.contains(shiftKey)
                            val isHighlighted = uiState.scrollToShiftKey == shiftKey
                            
                            val bgColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                animationSpec = androidx.compose.animation.core.tween(500)
                            )

                            Box(modifier = Modifier.background(bgColor)) {
                                ShiftRow(
                                    shift = shift,
                                    onStartClick = { viewModel.onEvent(ScheduleEvent.ToggleTimePicker(true, day, index, true)) },
                                    onEndClick = { viewModel.onEvent(ScheduleEvent.ToggleTimePicker(true, day, index, false)) },
                                    onRemove = { viewModel.onEvent(ScheduleEvent.RemoveShift(day, index)) },
                                    isError = isConflicting
                                )
                            }
                        }
                        if (daySchedule.shifts.isEmpty()) {
                            Text(Loc.Workday.noShiftsAdded, style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                        }
                    } else {
                        Text(Loc.Workday.off, style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50), modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun ShiftRow(
    shift: WorkShift,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onRemove: () -> Unit,
    isError: Boolean = false
) {
    val start = CalendarEventHelper.parseTime(shift.start)
    val end = CalendarEventHelper.parseTime(shift.end)
    
    val durationText = if (shift.start != null && shift.end != null) {
        var duration = Duration.between(start, end)
        if (duration.isNegative || duration.isZero) duration = duration.plusHours(24)
        
        val h = duration.toHours()
        val m = duration.toMinutes() % 60
        "(${h}${Loc.General.hourSymbol} ${m}${Loc.General.minuteSymbol})"
    } else ""

    val endsNextDay = shift.start != null && shift.end != null && (end.isBefore(start) || end == start)

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onStartClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = if (isError) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors()
        ) {
            Text(shift.start ?: Loc.General.none, fontSize = 13.sp)
        }
        
        Text("-", style = MaterialTheme.typography.bodyMedium)

        OutlinedButton(
            onClick = onEndClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = if (isError) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors()
        ) {
            Text(shift.end ?: Loc.General.none, fontSize = 13.sp)
        }

        Column(modifier = Modifier.width(80.dp)) {
            Text(durationText, style = MaterialTheme.typography.labelSmall, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            if (endsNextDay) {
                Text(Loc.Workday.nextDay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
}
