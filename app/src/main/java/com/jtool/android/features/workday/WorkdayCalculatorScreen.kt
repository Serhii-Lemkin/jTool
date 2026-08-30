package com.jtool.android.features.workday

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.ui.theme.JToolTheme
import com.jtool.android.core.components.layout.GenericTitle
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkdayCalculatorScreen(
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onBack: () -> Unit,
    viewModel: WorkdayViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                WorkdayViewModel(settingsManager, dataManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { LocalDate.now() }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currentLocale = remember(Loc.lang) { if (Loc.lang == "uk") Locale.forLanguageTag("uk") else Locale.ENGLISH }

    if (uiState.showEditDialog) {
        var tempHours by remember { mutableStateOf(uiState.weeklyHours) }
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(WorkdayEvent.ToggleEditDialog(false)) },
            title = { Text(Loc.worktime_editWorkHours) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    DayOfWeek.entries.forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Loc.getDayOfWeekName(day).take(3), modifier = Modifier.width(48.dp))
                            Slider(
                                value = (tempHours[day] ?: 0.0).toFloat(),
                                onValueChange = { tempHours = tempHours + (day to it.toDouble()) },
                                valueRange = 0f..12f,
                                steps = 23,
                                modifier = Modifier.weight(1f)
                            )
                            Text(String.format(currentLocale, "%.1f", tempHours[day]), modifier = Modifier.width(40.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onEvent(WorkdayEvent.SaveWeeklyHours(tempHours))
                }) { Text(Loc.general_save) }
            }
        )
    }

    if (uiState.showForgetConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(WorkdayEvent.ToggleForgetConfirm(false)) },
            title = { Text(Loc.worktime_forgetTitle) },
            text = { Text(Loc.worktime_forgetConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(WorkdayEvent.ConfirmForget)
                }) {
                    Text(Loc.general_delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(WorkdayEvent.ToggleForgetConfirm(false)) }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GenericTitle(
                title = Loc.worktime_workTime,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(WorkdayEvent.ToggleEditDialog(true)) }) {
                        Icon(Icons.Default.Settings, contentDescription = Loc.worktime_editHours)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Loc.worktime_today,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = Loc.getDayOfWeekName(uiState.currentDay),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${Loc.worktime_requiredHours}: ${uiState.requiredHours}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (uiState.requiredHours > 0.0) {
                OutlinedTextField(
                    value = uiState.inputTime,
                    onValueChange = { 
                        viewModel.onEvent(WorkdayEvent.UpdateInputTime(it))
                    },
                    label = { Text(Loc.worktime_startTime) },
                    placeholder = { Text("") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.onEvent(WorkdayEvent.Calculate)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Loc.worktime_calculate)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.onEvent(WorkdayEvent.ToggleRemember)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.isTimeSaved) Loc.worktime_forget else Loc.worktime_remember)
                    }
                }
            } else {
                Text(Loc.worktime_noWorkScheduled, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.errorText.isNotEmpty()) {
                Text(
                    text = uiState.errorText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.finishTime?.let { end ->
                val now = LocalTime.now()
                val duration = Duration.between(now, end)
                val hoursLeft = duration.toHours()
                val minutesLeft = duration.toMinutes() % 60
                
                val timeLeftText = when {
                    duration.isNegative || duration.isZero -> Loc.worktime_workFinished
                    hoursLeft > 0 -> "${Loc.worktime_timeLeft}: ${hoursLeft}${Loc.general_hourSymbol} ${minutesLeft}${Loc.general_minuteSymbol}"
                    else -> "${Loc.worktime_timeLeft}: ${minutesLeft}${Loc.general_minuteSymbol}"
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${Loc.worktime_finishAt}: ${end.format(timeFormatter)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeLeftText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WorkdayCalculatorScreenPreview() {
    val context = LocalContext.current
    JToolTheme {
        WorkdayCalculatorScreen(
            settingsManager = SettingsManager(context),
            dataManager = DataManager(context),
            onBack = {}
        )
    }
}

private fun parseTime(input: String): LocalTime? {
    val cleanInput = input.trim().replace(":", "").replace(".", "").replace(",", "")
    
    return when (cleanInput.length) {
        1 -> LocalTime.of(cleanInput.toInt(), 0)
        2 -> {
            val h = cleanInput.toInt()
            if (h < 24) LocalTime.of(h, 0) else null
        }
        3 -> {
            val h = cleanInput.substring(0, 1).toInt()
            val m = cleanInput.substring(1).toInt()
            if (m < 60) LocalTime.of(h, m) else null
        }
        4 -> {
            val h = cleanInput.substring(0, 2).toInt()
            val m = cleanInput.substring(2).toInt()
            if (h < 24 && m < 60) LocalTime.of(h, m) else null
        }
        else -> null
    }
}
