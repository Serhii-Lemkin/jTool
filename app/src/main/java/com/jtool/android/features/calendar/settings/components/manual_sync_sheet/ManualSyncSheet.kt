package com.jtool.android.features.calendar.settings.components.manual_sync_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSyncSheet(
    pullEnabled: Boolean,
    pushEnabled: Boolean,
    isSyncing: Boolean,
    syncStatus: String?,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var fromDate by remember { mutableStateOf(LocalDate.now().minusMonths(1)) }
    var toDate by remember { mutableStateOf(LocalDate.now().plusMonths(1)) }
    
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val syncActionText = when {
        pullEnabled && pushEnabled -> Loc.Calendar.manualSyncBoth
        pushEnabled -> Loc.Calendar.manualSyncPush
        else -> Loc.Calendar.manualSyncPull
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Calendar.manualSyncSheetTitle,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (syncStatus != null && !isSyncing) {
                val isError = syncStatus.contains("Failed") || syncStatus.contains("Помилка") || syncStatus.contains("נכשל")
                
                Icon(
                    imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = syncStatus,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(Loc.General.gotIt, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = Loc.Calendar.manualSyncSheetMsg.format(syncActionText),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = { if (!isSyncing) showFromPicker = true },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSyncing
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Loc.Workday.fromDate, style = MaterialTheme.typography.labelSmall)
                        Text(fromDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { if (!isSyncing) showToPicker = true },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isSyncing
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Loc.Workday.toDate, style = MaterialTheme.typography.labelSmall)
                        Text(toDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { onConfirm(fromDate, toDate) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(Loc.Calendar.syncing)
                    } else {
                        Text(Loc.General.select, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        fromDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showFromPicker = false
                }) { Text(Loc.General.save) }
            }
        ) { DatePicker(state = state) }
    }

    if (showToPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = toDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val selected = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        if (selected.isBefore(fromDate.plusYears(1))) {
                            toDate = selected
                        }
                    }
                    showToPicker = false
                }) { Text(Loc.General.save) }
            }
        ) { DatePicker(state = state) }
    }
}
