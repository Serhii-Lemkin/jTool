package com.jtool.android.features.cleaning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.CleaningTask
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
fun CleaningScreen(
    manager: CleaningManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
    viewModel: CleaningViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CleaningViewModel(manager, settingsManager, groupSyncManager)
            }
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (uiState.showDisabledView) Loc.showDisabled else Loc.cleaning,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            
            var menuExpandedTop by remember { mutableStateOf(false) }
            Box(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { menuExpandedTop = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(expanded = menuExpandedTop, onDismissRequest = { menuExpandedTop = false }) {
                    DropdownMenuItem(
                        text = { Text(Loc.addTask) },
                        onClick = {
                            viewModel.onEvent(CleaningEvent.ToggleAddDialog(true))
                            menuExpandedTop = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (uiState.showDisabledView) Loc.activeTasks else Loc.showDisabled) },
                        onClick = { 
                            viewModel.onEvent(CleaningEvent.ToggleDisabledView(!uiState.showDisabledView))
                            menuExpandedTop = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(uiState.displayTasks, key = { it.id }) { task ->
                CleaningTaskRow(
                    task = task,
                    onToggle = { checked ->
                        viewModel.onEvent(CleaningEvent.ToggleItem(task, checked))
                    },
                    onDelete = { viewModel.onEvent(CleaningEvent.RequestDelete(task)) },
                    onDisable = { isEnabled ->
                        viewModel.onEvent(CleaningEvent.UpdateTaskStatus(task, isEnabled))
                    },
                    onEdit = { viewModel.onEvent(CleaningEvent.RequestEdit(task)) },
                    userName = uiState.userNames[task.lastUpdatedBy] ?: task.lastUpdatedBy
                )
            }
        }
    }

    if (uiState.showAddDialog) {
        var addTitle by remember { mutableStateOf("") }
        var addDays by remember { mutableStateOf("7") }
        var addRecurring by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CleaningEvent.ToggleAddDialog(false)) },
            title = { Text(Loc.addTask) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addTitle,
                        onValueChange = { addTitle = it },
                        label = { Text(Loc.taskTitle) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addDays,
                        onValueChange = { if (it.all { c -> c.isDigit() }) addDays = it },
                        label = { Text(Loc.everyXDays) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.recurring)
                        Checkbox(checked = addRecurring, onCheckedChange = { addRecurring = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addTitle.isNotBlank()) {
                            viewModel.onEvent(CleaningEvent.AddTask(addTitle, addDays.toIntOrNull() ?: 1, addRecurring))
                        }
                    },
                    enabled = addTitle.isNotBlank()
                ) {
                    Text(Loc.add)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(CleaningEvent.ToggleAddDialog(false)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (uiState.taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CleaningEvent.RequestDelete(null)) },
            title = { Text(Loc.confirmDeleteTitle) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(CleaningEvent.ConfirmDelete)
                }) {
                    Text(Loc.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(CleaningEvent.RequestDelete(null)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (uiState.editingTask != null) {
        var editTitle by remember { mutableStateOf(uiState.editingTask!!.title) }
        var editDays by remember { mutableStateOf(uiState.editingTask!!.frequencyDays.toString()) }
        var editRecurring by remember { mutableStateOf(uiState.editingTask!!.isRecurring) }

        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CleaningEvent.RequestEdit(null)) },
            title = { Text(Loc.editTask) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text(Loc.taskTitle) })
                    OutlinedTextField(value = editDays, onValueChange = { editDays = it }, label = { Text(Loc.everyXDays) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.recurring)
                        Checkbox(checked = editRecurring, onCheckedChange = { editRecurring = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onEvent(CleaningEvent.SaveTask(editTitle, editDays.toIntOrNull() ?: 1, editRecurring))
                }) { Text(Loc.save) }
            }
        )
    }
}

@Composable
fun CleaningTaskRow(
    task: CleaningTask,
    onToggle: (Boolean) -> Unit,
    onDisable: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    userName: String
) {
    val currentLocale = remember(Loc.lang) { if (Loc.lang == "uk") Locale.forLanguageTag("uk") else Locale.ENGLISH }
    val dateFormat = remember(currentLocale) { SimpleDateFormat("MMM dd, HH:mm", currentLocale) }
    var menuExpanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.isEnabled) {
                    Checkbox(checked = task.isChecked, onCheckedChange = onToggle)
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (task.isEnabled) MaterialTheme.colorScheme.onSurface 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = if (task.isRecurring) Loc.getCleaningFrequency(task.frequencyDays) else Loc.oneTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (task.isEnabled) {
                            DropdownMenuItem(text = { Text(Loc.update) }, onClick = { onEdit(); menuExpanded = false })
                            DropdownMenuItem(text = { Text(Loc.disable) }, onClick = { onDisable(false); menuExpanded = false })
                            DropdownMenuItem(text = { Text(Loc.delete, color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); menuExpanded = false })
                        } else {
                            DropdownMenuItem(text = { Text(Loc.restoreTask) }, onClick = { onDisable(true); menuExpanded = false })
                            DropdownMenuItem(text = { Text(Loc.delete, color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); menuExpanded = false })
                        }
                    }
                }
            }
            
            if (task.lastDoneTimestamp > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 4.dp), 
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Loc.lastDone}: ${dateFormat.format(Date(task.lastDoneTimestamp))} ${Loc.by} $userName",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CleaningPreview() {
    val context = LocalContext.current
    MaterialTheme {
        CleaningScreen(
            manager = CleaningManager(context),
            backupManager = BackupManager(context),
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context)
        )
    }
}
