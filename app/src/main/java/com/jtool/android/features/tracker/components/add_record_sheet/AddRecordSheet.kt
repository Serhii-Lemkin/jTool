package com.jtool.android.features.tracker.components.add_record_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.utils.rememberCustomDatePickerState
import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.core.models.tracker.TrackerRecord
import com.jtool.android.ui.components.common.GenericSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordSheet(
    tracker: Tracker,
    onDismiss: () -> Unit,
    onAdd: (TrackerRecord) -> Unit,
    userId: String,
    userName: String,
    editingRecord: TrackerRecord? = null
) {
    val metricValues = remember { 
        val map = mutableStateMapOf<String, String>()
        editingRecord?.values?.forEach { (id, value) -> map[id] = value.toString() }
        map
    }
    var selectedTimestamp by remember { mutableLongStateOf(editingRecord?.timestamp ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberCustomDatePickerState(initialSelectedDateMillis = selectedTimestamp)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedTimestamp = it }
                    showDatePicker = false
                }) {
                    Text(Loc.General.gotIt)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(Loc.General.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = tracker.title,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // User attribution
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(Loc.Tracker.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = remember(selectedTimestamp, Loc.lang) {
                                SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag(Loc.lang)).format(Date(selectedTimestamp))
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracker.metrics) { metric ->
                    OutlinedTextField(
                        value = metricValues[metric.id] ?: "",
                        onValueChange = { metricValues[metric.id] = it },
                        label = { Text(metric.name + (metric.unit?.let { " ($it)" } ?: "")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val valuesMap = metricValues.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                        .filter { it.value != 0.0 }
                    
                    if (valuesMap.isNotEmpty()) {
                        onAdd(
                            TrackerRecord(
                                id = editingRecord?.id ?: UUID.randomUUID().toString(),
                                trackerId = tracker.id,
                                userId = editingRecord?.userId ?: userId,
                                userName = editingRecord?.userName ?: userName,
                                timestamp = selectedTimestamp,
                                values = valuesMap
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = metricValues.values.any { it.isNotBlank() && it.toDoubleOrNull() != null },
                shape = MaterialTheme.shapes.medium
            ) {
                Text(Loc.General.save, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
