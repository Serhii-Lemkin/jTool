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
import com.jtool.android.core.components.layout.GenericTitle
import java.time.format.DateTimeFormatter

import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCalculatorScreen(
    onBack: () -> Unit,
    viewModel: TimeCalcViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    Scaffold(
        topBar = {
            GenericTitle(
                title = Loc.timecalc_timeCalc,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(TimeCalcEvent.ResetToNow) }) {
                        Icon(Icons.Default.Refresh, contentDescription = Loc.worktime_resetToNow)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section: Set Base Time
            Text(Loc.timecalc_baseDateTime, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TimeInputField(Loc.timecalc_year, uiState.bY, Modifier.weight(1.2f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(y = it)) }
                TimeInputField(Loc.timecalc_month, uiState.bMo, Modifier.weight(0.8f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(mo = it)) }
                TimeInputField(Loc.timecalc_day, uiState.bD, Modifier.weight(0.8f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(d = it)) }
                TimeInputField(Loc.timecalc_hour, uiState.bH, Modifier.weight(0.8f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(h = it)) }
                TimeInputField(Loc.timecalc_minute, uiState.bMi, Modifier.weight(0.8f)) { viewModel.onEvent(TimeCalcEvent.UpdateBase(mi = it)) }
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
            Text(Loc.timecalc_addSub, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TimeInputField(Loc.general_yearShort.uppercase(), uiState.yearsInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(y = it)) }
                TimeInputField(Loc.general_monthShort.uppercase(), uiState.monthsInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(mo = it)) }
                TimeInputField(Loc.general_dayShort.uppercase(), uiState.daysInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(d = it)) }
                TimeInputField(Loc.general_hourSymbol.uppercase(), uiState.hoursInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(h = it)) }
                TimeInputField(Loc.general_minuteSymbol.uppercase(), uiState.minutesInput, Modifier.weight(1f)) { viewModel.onEvent(TimeCalcEvent.UpdateOperation(mi = it)) }
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
                }

                Button(
                    onClick = {
                        viewModel.onEvent(TimeCalcEvent.PerformOperation(false))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Remove, null)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(Loc.timecalc_history, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val reversedHistory = uiState.history.reversed()
                itemsIndexed(reversedHistory) { _, item ->
                    Card(
                        onClick = { viewModel.onEvent(TimeCalcEvent.RestoreFromHistory(item)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("${item.operation}: ${item.summary}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${Loc.timecalc_resultedIn}: ${item.baseDate.format(formatter)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                item {
                    Spacer(Modifier.height(100.dp))
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

@Preview(showBackground = true)
@Composable
fun TimeCalculatorPreview() {
    JToolTheme { TimeCalculatorScreen(onBack = {}) }
}
