package com.jtool.android.features.calendar.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.components.AddEditEventDialog
import com.jtool.android.features.calendar.components.delete_event_confirm_sheet.DeleteEventConfirmSheet
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventDetailScreen(
    onBack: () -> Unit,
    viewModel: CalendarEventDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val event = uiState.event
    var showDeleteSheet by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (showDeleteSheet) {
        DeleteEventConfirmSheet(
            onDismiss = { showDeleteSheet = false },
            onConfirm = {
                viewModel.deleteEvent()
                onBack()
            }
        )
    }
    
    if (uiState.showEditDialog && event != null) {
        AddEditEventDialog(
            event = event,
            selectedDate = LocalDate.parse(event.date),
            onDismiss = { viewModel.toggleEditDialog(false) },
            onConfirm = { updated, isShared -> 
                viewModel.updateEvent(updated, isShared)
            }
        )
    }

    Scaffold(
        topBar = {
            GenericTitle(
                title = event?.title?.ifEmpty { Loc.Calendar.calendar } ?: Loc.Calendar.calendar,
                onBack = onBack
            )
        }
    ) { padding ->
        if (event == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Loc.Calendar.noEventFound)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(Icons.Default.Event, Loc.Calendar.date, event.date)
                InfoRow(Icons.Default.AccessTime, Loc.Calendar.time, "${event.startTime}${event.endTime?.let { " - $it" } ?: ""}")
                InfoRow(
                    imageVector = if (event.type == CalendarEventTypeEnum.SHIFT) Icons.Default.Work else Icons.Default.Category,
                    label = Loc.Calendar.type,
                    value = if (event.type == CalendarEventTypeEnum.SHIFT) Loc.Calendar.shift else Loc.Calendar.common
                )
                
                if (event.note.isNotEmpty()) {
                    InfoRow(Icons.Default.Notes, Loc.Calendar.note, event.note)
                }

                Spacer(Modifier.weight(1f))
                
                if (!event.id.startsWith("virtual_")) {
                    Button(
                        onClick = { viewModel.toggleEditDialog(true) },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text(Loc.General.update)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { showDeleteSheet = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text(Loc.General.delete)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(imageVector: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}
