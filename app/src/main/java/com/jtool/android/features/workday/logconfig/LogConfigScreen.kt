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
import kotlinx.coroutines.launch
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
            viewModel.onEvent(LogConfigEvent.ToggleExitConfirm(show = true))
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
                    onClick = { viewModel.onEvent(LogConfigEvent.ToggleExitConfirm(false)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Loc.Workday.keepEditing)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            GenericTitle(
                title = Loc.Workday.manual,
                onBack = handleBack,
                actions = {
                    val scope = rememberCoroutineScope()
                    IconButton(onClick = { 
                        scope.launch {
                            viewModel.onEvent(LogConfigEvent.Save)
                            onBack()
                        }
                    }) {
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
            Text(
                text = Loc.Workday.setWorkHoursDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            val sortedDays = remember(uiState.weeklyStartDay) {
                val all = DayOfWeek.entries
                val startIndex = all.indexOf(uiState.weeklyStartDay)
                all.subList(startIndex, all.size) + all.subList(0, startIndex)
            }

            sortedDays.forEach { day ->
                val logHours = uiState.tempWeeklySchedule[day] ?: DayLogHours()
                val isSet = (logHours.hours > 0) || (logHours.minutes > 0)
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
                    }
                    
                    if (!logHours.isConfirmedOff) {
                        Row(modifier = Modifier.width(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = if (logHours.hours == 0) "" else logHours.hours.toString(),
                                onValueChange = { s -> 
                                    val v = s.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    if (v < 24) viewModel.onEvent(LogConfigEvent.UpdateTempSchedule(day, logHours.copy(hours = v)))
                                },
                                label = { Text(Loc.General.hourSymbol.uppercase(), fontSize = 10.sp) },
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
                                label = { Text(Loc.General.minuteSymbol.uppercase(), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                        }
                    } else {
                        Text(
                            Loc.Workday.off, 
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
