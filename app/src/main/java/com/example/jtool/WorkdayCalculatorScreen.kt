package com.example.jtool

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
import com.example.jtool.ui.theme.JToolTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WorkdayCalculatorScreen(
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager // Added
) {
    var weeklyHours by remember { mutableStateOf(settingsManager.getAllWeeklyHours()) }
    var showForgetConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isTimeSaved by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val currentDay = today.dayOfWeek
    val requiredHours = weeklyHours[currentDay] ?: 0.0

    var inputTime by remember { mutableStateOf("") }
    var finishTime by remember { mutableStateOf<LocalTime?>(null) }
    var errorText by remember { mutableStateOf("") }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currentLocale = remember(Loc.lang) { if (Loc.lang == "uk") Locale.forLanguageTag("uk") else Locale.ENGLISH }

    LaunchedEffect(Unit) {
        settingsManager.getRememberedStart(today.toString())?.let { remembered ->
            inputTime = remembered
            isTimeSaved = true
            val parsed = parseTime(remembered)
            if (parsed != null) {
                val totalMinutes = (requiredHours * 60).toLong()
                finishTime = parsed.plusMinutes(totalMinutes)
            }
        }
        
        // Show prompt if no hours set at all
        if (weeklyHours.values.all { it == 0.0 }) {
            showEditDialog = true
        }
    }

    if (showEditDialog) {
        var tempHours by remember { mutableStateOf(weeklyHours) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(Loc.editWorkHours) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(Loc.setHoursManually, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    DayOfWeek.values().forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(day.getDisplayName(TextStyle.SHORT, currentLocale), modifier = Modifier.width(40.dp))
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
                    tempHours.forEach { (day, h) -> settingsManager.saveDayHours(day, h) }
                    weeklyHours = tempHours
                    scope.launch { groupSyncManager.pushUserSettings() }
                    showEditDialog = false
                }) { Text(Loc.save) }
            }
        )
    }

    val currentInvalidFormat = Loc.invalidFormat

    if (showForgetConfirm) {
        AlertDialog(
            onDismissRequest = { showForgetConfirm = false },
            title = { Text(Loc.forgetTitle) },
            text = { Text(Loc.forgetConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    settingsManager.clearRememberedStart()
                    isTimeSaved = false
                    showForgetConfirm = false
                }) {
                    Text(Loc.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetConfirm = false }) {
                    Text(Loc.cancel)
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(48.dp))
            Text(
                text = Loc.today,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = Loc.editHours)
            }
        }
        
        Text(
            text = currentDay.getDisplayName(java.time.format.TextStyle.FULL, currentLocale),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "${Loc.requiredHours}: $requiredHours",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (requiredHours > 0.0) {
            OutlinedTextField(
                value = inputTime,
                onValueChange = { 
                    inputTime = it
                    if (errorText.isNotEmpty()) errorText = "" 
                },
                label = { Text(Loc.startTime) },
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
                        val parsed = parseTime(inputTime)
                        if (parsed != null) {
                            val totalMinutes = (requiredHours * 60).toLong()
                            finishTime = parsed.plusMinutes(totalMinutes)
                            errorText = ""
                        } else {
                            finishTime = null
                            errorText = currentInvalidFormat
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Loc.calculate)
                }

                OutlinedButton(
                    onClick = {
                        if (isTimeSaved) {
                            showForgetConfirm = true
                        } else {
                            settingsManager.saveRememberedStart(inputTime, today.toString())
                            isTimeSaved = true
                            // Also trigger calculation when remembering
                            val parsed = parseTime(inputTime)
                            if (parsed != null) {
                                val totalMinutes = (requiredHours * 60).toLong()
                                finishTime = parsed.plusMinutes(totalMinutes)
                                errorText = ""
                            } else {
                                errorText = currentInvalidFormat
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isTimeSaved) Loc.forget else Loc.remember)
                }
            }
        } else {
            Text(Loc.noWorkScheduled, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (errorText.isNotEmpty()) {
            Text(
                text = errorText,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error
            )
        }

        finishTime?.let { end ->
            val now = LocalTime.now()
            val duration = Duration.between(now, end)
            val hoursLeft = duration.toHours()
            val minutesLeft = duration.toMinutes() % 60
            
            val timeLeftText = when {
                duration.isNegative || duration.isZero -> Loc.workFinished
                hoursLeft > 0 -> "${Loc.timeLeft}: ${hoursLeft}${Loc.hourSymbol} ${minutesLeft}${Loc.minuteSymbol}"
                else -> "${Loc.timeLeft}: ${minutesLeft}${Loc.minuteSymbol}"
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${Loc.finishAt}: ${end.format(timeFormatter)}",
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
    }
}

@Preview(showBackground = true)
@Composable
fun WorkdayCalculatorPreview() {
    val context = LocalContext.current
    JToolTheme {
        WorkdayCalculatorScreen(
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context)
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
