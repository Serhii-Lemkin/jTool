package com.example.jtool

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
import com.example.jtool.ui.theme.JToolTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TimeCalculatorScreen() {
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    val history = remember { mutableStateListOf<HistoryItem>() }

    // Base Time Inputs
    var bY by remember { mutableStateOf(currentDateTime.year.toString()) }
    var bMo by remember { mutableStateOf(currentDateTime.monthValue.toString()) }
    var bD by remember { mutableStateOf(currentDateTime.dayOfMonth.toString()) }
    var bH by remember { mutableStateOf(currentDateTime.hour.toString()) }
    var bMi by remember { mutableStateOf(currentDateTime.minute.toString()) }

    // Operation Inputs
    var yearsInput by remember { mutableStateOf("") }
    var monthsInput by remember { mutableStateOf("") }
    var daysInput by remember { mutableStateOf("") }
    var hoursInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("") }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // Synchronize base inputs when currentDateTime changes (e.g., from history or operations)
    LaunchedEffect(currentDateTime) {
        bY = currentDateTime.year.toString()
        bMo = currentDateTime.monthValue.toString()
        bD = currentDateTime.dayOfMonth.toString()
        bH = currentDateTime.hour.toString()
        bMi = currentDateTime.minute.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentDateTime = LocalDateTime.now() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset to Now")
            }
        }

        Text(
            text = "jTimeCalc",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Section: Set Base Time
        Text(Loc.baseDateTime, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimeInputField(Loc.t("Year", "Рік"), bY, Modifier.weight(1.2f)) { bY = it; updateBase(bY, bMo, bD, bH, bMi) { currentDateTime = it } }
            TimeInputField(Loc.t("Mo", "Міс"), bMo, Modifier.weight(0.8f)) { bMo = it; updateBase(bY, bMo, bD, bH, bMi) { currentDateTime = it } }
            TimeInputField(Loc.t("Day", "День"), bD, Modifier.weight(0.8f)) { bD = it; updateBase(bY, bMo, bD, bH, bMi) { currentDateTime = it } }
            TimeInputField(Loc.t("Hr", "Год"), bH, Modifier.weight(0.8f)) { bH = it; updateBase(bY, bMo, bD, bH, bMi) { currentDateTime = it } }
            TimeInputField(Loc.t("Min", "Хв"), bMi, Modifier.weight(0.8f)) { bMi = it; updateBase(bY, bMo, bD, bH, bMi) { currentDateTime = it } }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Display
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = currentDateTime.format(formatter),
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
            TimeInputField("Y", yearsInput, Modifier.weight(1f)) { yearsInput = it }
            TimeInputField("M", monthsInput, Modifier.weight(1f)) { monthsInput = it }
            TimeInputField("D", daysInput, Modifier.weight(1f)) { daysInput = it }
            TimeInputField("H", hoursInput, Modifier.weight(1f)) { hoursInput = it }
            TimeInputField("M", minutesInput, Modifier.weight(1f)) { minutesInput = it }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val next = calculateNewDate(currentDateTime, yearsInput, monthsInput, daysInput, hoursInput, minutesInput, true)
                    history.add(HistoryItem(currentDateTime, Loc.added, yearsInput, monthsInput, daysInput, hoursInput, minutesInput))
                    currentDateTime = next
                    yearsInput = ""; monthsInput = ""; daysInput = ""; hoursInput = ""; minutesInput = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, null)
                Text(Loc.add)
            }

            Button(
                onClick = {
                    val next = calculateNewDate(currentDateTime, yearsInput, monthsInput, daysInput, hoursInput, minutesInput, false)
                    history.add(HistoryItem(currentDateTime, Loc.subtracted, yearsInput, monthsInput, daysInput, hoursInput, minutesInput))
                    currentDateTime = next
                    yearsInput = ""; monthsInput = ""; daysInput = ""; hoursInput = ""; minutesInput = ""
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
            itemsIndexed(history.reversed()) { _, item ->
                Card(
                    onClick = { currentDateTime = item.baseDate },
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
        if (y.isNotEmpty()) parts.add("${y}y")
        if (mo.isNotEmpty()) parts.add("${mo}mo")
        if (d.isNotEmpty()) parts.add("${d}d")
        if (h.isNotEmpty()) parts.add("${h}h")
        if (mi.isNotEmpty()) parts.add("${mi}m")
        return if (parts.isEmpty()) "0 units" else parts.joinToString(" ")
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
