package com.jtool.android.features.timecalc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.theme.JToolTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TimeCalculatorScreen(
    viewModel: TimeCalcViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.onEvent(TimeCalcEvent.ResetToNow) }) {
                Icon(Icons.Default.Refresh, contentDescription = Loc.resetToNow)
            }
        }

        Text(
            text = Loc.timeCalc,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Section: Set Base Time
        Text(Loc.baseDateTime, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimeInputField(Loc.t("Year", "Рік"), uiState.bY, Modifier.weight(1.2f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(y = it)) }
            TimeInputField(Loc.t("Mo", "Міс"), uiState.bMo, Modifier.weight(0.8f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(mo = it)) }
            TimeInputField(Loc.t("Day", "День"), uiState.bD, Modifier.weight(0.8f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(d = it)) }
            TimeInputField(Loc.t("Hr", "Год"), uiState.bH, Modifier.weight(0.8f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(h = it)) }
            TimeInputField(Loc.t("Min", "Хв"), uiState.bMi, Modifier.weight(0.8f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(mi = it)) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Display
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.currentDateTime.format(formatter),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Operations
        Text(Loc.addSub, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimeInputField(Loc.yearShort.uppercase(), uiState.yearsInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(y = it)) }
            TimeInputField(Loc.monthShort.uppercase(), uiState.monthsInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(mo = it)) }
            TimeInputField(Loc.dayShort.uppercase(), uiState.daysInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(d = it)) }
            TimeInputField(Loc.hourSymbol.uppercase(), uiState.hoursInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(h = it)) }
            TimeInputField(Loc.minuteSymbol.uppercase(), uiState.minutesInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(mi = it)) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    viewModel.onEvent(TimeCalcEvent.PerformOperation(true))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, null)
                Text(Loc.add)
            }

            Button(
                onClick = {
                    viewModel.onEvent(TimeCalcEvent.PerformOperation(false))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Remove, null)
                Text(Loc.sub)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(Loc.history, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(uiState.history.reversed()) { _, item ->
                Card(
                    onClick = { viewModel.onEvent(TimeCalcEvent.RestoreFromHistory(item)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("${item.operation}: ${item.summary}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${Loc.resultedIn}: ${item.baseDate.format(formatter)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeInputField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue -> if (newValue.isEmpty() || newValue.all { char -> char.isDigit() }) onValueChange(newValue) },
        label = { Text(label, fontSize = 9.sp) },
        modifier = modifier,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
    )
}

private fun updateBase(y: String, mo: String, d: String, h: String, mi: String, onUpdate: (LocalDateTime) -> Unit) {
    try {
        val year = y.toIntOrNull() ?: 2024
        val month = (mo.toIntOrNull() ?: 1).coerceIn(1, 12)
        val day = (d.toIntOrNull() ?: 1).coerceIn(1, 31)
        val hour = (h.toIntOrNull() ?: 0).coerceIn(0, 23)
        val min = (mi.toIntOrNull() ?: 0).coerceIn(0, 59)
        
        // Basic check for February/31sts to avoid crash
        val tempDate = LocalDateTime.of(year, month, 1, hour, min)
        val finalDay = day.coerceIn(1, tempDate.toLocalDate().lengthOfMonth())
        onUpdate(LocalDateTime.of(year, month, finalDay, hour, min))
    } catch (_: Exception) {}
}

data class HistoryItem(
    val baseDate: LocalDateTime,
    val operation: String,
    val y: String, val mo: String, val d: String, val h: String, val mi: String
) {
    val summary: String get() {
        val parts = mutableListOf<String>()
        if (y.isNotEmpty()) parts.add("${y}${Loc.yearShort}")
        if (mo.isNotEmpty()) parts.add("${mo}${Loc.monthShort}")
        if (d.isNotEmpty()) parts.add("${d}${Loc.dayShort}")
        if (h.isNotEmpty()) parts.add("${h}${Loc.hourSymbol}")
        if (mi.isNotEmpty()) parts.add("${mi}${Loc.minuteSymbol}")
        return if (parts.isEmpty()) Loc.zeroUnits else parts.joinToString(" ")
    }
}

private fun calculateNewDate(base: LocalDateTime, y: String, mo: String, d: String, h: String, mi: String, isAddition: Boolean): LocalDateTime {
    var result = base
    val m = if (isAddition) 1L else -1L
    if (y.isNotEmpty()) result = result.plusYears(y.toLong() * m)
    if (mo.isNotEmpty()) result = result.plusMonths(mo.toLong() * m)
    if (d.isNotEmpty()) result = result.plusDays(d.toLong() * m)
    if (h.isNotEmpty()) result = result.plusHours(h.toLong() * m)
    if (mi.isNotEmpty()) result = result.plusMinutes(mi.toLong() * m)
    return result
}

@Preview(showBackground = true)
@Composable
fun TimeCalculatorPreview() {
    JToolTheme { TimeCalculatorScreen() }
}
