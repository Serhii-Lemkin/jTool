package com.jtool.android.features.workday.logconfig

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.workday.DayLogHours
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogConfigScreen(
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onBack: () -> Unit,
    viewModel: LogConfigViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                LogConfigViewModel(settingsManager, dataManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val handleBack = {
        if (uiState.hasUnsavedChanges) {
            viewModel.onEvent(LogConfigEvent.ToggleExitConfirm(true))
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    if (uiState.showExitConfirm) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(LogConfigEvent.ToggleExitConfirm(false)) },
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
                    text = Loc.worktime_unsavedChangesTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Loc.worktime_unsavedChangesMsg,
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
                    Text(Loc.worktime_exit)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.onEvent(LogConfigEvent.ToggleExitConfirm(false)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Loc.worktime_keepEditing)
                }
            }
        }
    }

    if (uiState.showApproximateHoursPicker) {
        ApproximateHoursBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.onEvent(LogConfigEvent.ToggleApproximateHoursPicker(false)) },
            onUpdateConfig = { useStable, start, end -> 
                viewModel.onEvent(LogConfigEvent.UpdateApproximateHoursConfig(useStable, start, end))
            },
            onUpdateDay = { day, hours -> 
                viewModel.updatePendingDay(day, hours)
            },
            settingsManager = settingsManager
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            GenericTitle(
                title = Loc.worktime_manual,
                onBack = handleBack,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(LogConfigEvent.Save); onBack() }) {
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
                .padding(vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            ActionListItem(
                headline = Loc.worktime_approximateHours,
                supportingText = Loc.worktime_approximateHoursExpl,
                leadingContent = { Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { viewModel.onEvent(LogConfigEvent.ToggleApproximateHoursPicker(true)) },
                supportingMaxLines = Int.MAX_VALUE
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            val sortedDays = remember(uiState.weeklyStartDay) {
                val all = DayOfWeek.entries
                val startIndex = all.indexOf(uiState.weeklyStartDay)
                all.subList(startIndex, all.size) + all.subList(0, startIndex)
            }

            sortedDays.forEach { day ->
                val logHours = uiState.tempWeeklySchedule[day] ?: DayLogHours()
                val isSet = logHours.hours > 0 || logHours.minutes > 0
                val dayColor = if (logHours.isConfirmedOff) Color(0xFF4CAF50) else if (isSet) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Loc.getDayOfWeekName(day),
                            fontWeight = FontWeight.Bold,
                            color = dayColor
                        )
                        val approxStart = if (uiState.useStableApproximateHours) uiState.stableApproximateStart else logHours.approximateStartTime
                        val approxEnd = if (uiState.useStableApproximateHours) uiState.stableApproximateEnd else logHours.approximateEndTime
                        
                        if (!logHours.isConfirmedOff && !approxStart.isNullOrEmpty()) {
                            Text(
                                text = "$approxStart - ${approxEnd ?: "??"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    if (!logHours.isConfirmedOff) {
                        Row(modifier = Modifier.width(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = if (logHours.hours == 0) "" else logHours.hours.toString(),
                                onValueChange = { s -> 
                                    val v = s.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    if (v < 24) viewModel.onEvent(LogConfigEvent.UpdateTempSchedule(day, logHours.copy(hours = v)))
                                },
                                label = { Text(Loc.general_hourSymbol.uppercase(), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            OutlinedTextField(
                                value = if (logHours.minutes == 0) "" else logHours.minutes.toString(),
                                onValueChange = { s -> 
                                    val v = s.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    if (v < 60) viewModel.onEvent(LogConfigEvent.UpdateTempSchedule(day, logHours.copy(minutes = v)))
                                },
                                label = { Text(Loc.general_minuteSymbol.uppercase(), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                        }
                    } else {
                        Text(
                            Loc.worktime_off, 
                            style = MaterialTheme.typography.labelMedium, 
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }

                    IconButton(onClick = { 
                        viewModel.onEvent(LogConfigEvent.UpdateTempSchedule(day, logHours.copy(isConfirmedOff = !logHours.isConfirmedOff)))
                    }) {
                        Icon(
                            if (logHours.isConfirmedOff) Icons.Default.EventBusy else Icons.Default.EventAvailable,
                            null,
                            tint = if (logHours.isConfirmedOff) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproximateHoursBottomSheet(
    uiState: LogConfigUiState,
    onDismiss: () -> Unit,
    onUpdateConfig: (Boolean, String?, String?) -> Unit,
    onUpdateDay: (DayOfWeek, DayLogHours) -> Unit,
    settingsManager: SettingsManager
) {
    val options = remember { settingsManager.getWorktimeOptions() }
    val workHours = (options.freehandDurationHours * 60) + options.freehandDurationMinutes
    
    var pickingForStart by remember { mutableStateOf<Any?>(null) } // true = stable, DayOfWeek = individual

    if (pickingForStart != null) {
        key(pickingForStart) {
            val initialHour = 9
            val timePickerState = rememberTimePickerState(
                initialHour = initialHour,
                initialMinute = 0,
                is24Hour = true
            )
            
            val dayForTitle = if (pickingForStart is DayOfWeek) Loc.getDayOfWeekName(pickingForStart as DayOfWeek) else ""
            
            ModalBottomSheet(
                onDismissRequest = { pickingForStart = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (dayForTitle.isNotEmpty()) "Select Start Time for $dayForTitle" else "Select Start Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        val start = String.format(Locale.ENGLISH, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                        val end = LocalTime.of(timePickerState.hour, timePickerState.minute).plusMinutes(workHours.toLong()).toString()
                        
                        if (pickingForStart == true) {
                            onUpdateConfig(true, start, end)
                        } else if (pickingForStart is DayOfWeek) {
                            val day = pickingForStart as DayOfWeek
                            val logHours = uiState.pendingWeeklySchedule[day] ?: DayLogHours()
                            onUpdateDay(day, logHours.copy(approximateStartTime = start, approximateEndTime = end))
                        }
                        pickingForStart = null
                    }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(Loc.general_select)
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Loc.worktime_hoursConfig,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(24.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.pendingUseStable,
                    onClick = { onUpdateConfig(true, null, null) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(Loc.worktime_stableHours)
                }
                SegmentedButton(
                    selected = !uiState.pendingUseStable,
                    onClick = { onUpdateConfig(false, null, null) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(Loc.worktime_setByDay)
                }
            }

            Spacer(Modifier.height(24.dp))

            androidx.compose.animation.AnimatedContent(
                targetState = uiState.pendingUseStable,
                label = "approx_hours_transition"
            ) { useStable ->
                if (useStable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { pickingForStart = true },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.AccessTime, null)
                            Spacer(Modifier.width(8.dp))
                            Text(uiState.pendingStableStart ?: Loc.worktime_start)
                        }
                        
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.pendingStableEnd ?: "--:--",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            val logHours = uiState.pendingWeeklySchedule[day] ?: DayLogHours()
                            val isOff = logHours.isConfirmedOff
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Loc.getDayOfWeekName(day),
                                    modifier = Modifier.weight(1.5f),
                                    style = if (isOff) MaterialTheme.typography.bodyMedium.copy(color = Color.Gray) else MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                if (!isOff) {
                                    Row(modifier = Modifier.weight(2.5f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = { pickingForStart = day },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(4.dp),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(logHours.approximateStartTime ?: "Start", fontSize = 13.sp)
                                        }
                                        
                                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)

                                        Text(
                                            text = logHours.approximateEndTime ?: "--:--",
                                            modifier = Modifier.weight(1f),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = Loc.worktime_off,
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(Loc.general_close)
            }
        }
    }
}
