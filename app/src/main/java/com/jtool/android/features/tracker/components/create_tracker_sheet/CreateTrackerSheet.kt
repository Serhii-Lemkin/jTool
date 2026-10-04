package com.jtool.android.features.tracker.components.create_tracker_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.core.models.tracker.TrackerMetric
import com.jtool.android.features.tracker.components.delete_metric_confirm_sheet.DeleteMetricConfirmSheet
import com.jtool.android.ui.components.common.GenericSheet
import java.util.UUID

@Composable
fun CreateTrackerSheet(
    editingTracker: Tracker? = null,
    onDismiss: () -> Unit,
    onConfirm: (Tracker) -> Unit
) {
    var title by remember { mutableStateOf(editingTracker?.title ?: "") }
    var metrics by remember { mutableStateOf(editingTracker?.metrics ?: listOf<TrackerMetric>()) }
    
    var newMetricName by remember { mutableStateOf("") }
    var newMetricUnit by remember { mutableStateOf("") }
    var metricToDelete by remember { mutableStateOf<TrackerMetric?>(null) }

    GenericSheet(
        onDismiss = onDismiss,
        title = if (editingTracker == null) Loc.Tracker.createTracker else Loc.Tracker.editTracker,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(Loc.Tracker.trackerTitle) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMetricName,
                    onValueChange = { newMetricName = it },
                    label = { Text(Loc.Tracker.metricName) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = newMetricUnit,
                    onValueChange = { newMetricUnit = it },
                    label = { Text(Loc.Tracker.metricUnit) },
                    modifier = Modifier.width(70.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                IconButton(
                    onClick = {
                        if (newMetricName.isNotBlank()) {
                            metrics = metrics + TrackerMetric(
                                id = UUID.randomUUID().toString(),
                                name = newMetricName,
                                unit = newMetricUnit.takeIf { it.isNotBlank() }
                            )
                            newMetricName = ""
                            newMetricUnit = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (metrics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(metrics) { metric ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(metric.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    metric.unit?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                IconButton(onClick = { metricToDelete = metric }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (title.isNotBlank() && metrics.isNotEmpty()) {
                        val result = editingTracker?.copy(title = title, metrics = metrics) 
                            ?: Tracker(title = title, metrics = metrics)
                        onConfirm(result)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = title.isNotBlank() && metrics.isNotEmpty(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(Loc.General.save, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (metricToDelete != null) {
        DeleteMetricConfirmSheet(
            onDismiss = { metricToDelete = null },
            onConfirm = {
                metrics = metrics.filter { it.id != metricToDelete!!.id }
                metricToDelete = null
            }
        )
    }
}
