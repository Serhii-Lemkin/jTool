package com.jtool.android.features.cleaning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.jtool.android.core.components.layout.CompactActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.components.layout.UnifiedFAB
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningScreen(
    manager: CleaningManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
    onBack: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                GenericTitle(
                    title = if (uiState.showDisabledView) Loc.cleaning_showDisabled else Loc.cleaning_cleaning,
                    onBack = onBack,
                    isLoading = uiState.isLoading,
                    onRefresh = { viewModel.onEvent(CleaningEvent.Refresh) },
                    actions = {
                        TitleActionIcon(
                            icon = if (uiState.showDisabledView) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            onClick = { viewModel.onEvent(CleaningEvent.ToggleDisabledView(!uiState.showDisabledView)) },
                            tint = if (uiState.showDisabledView) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                )
            }
        ) { padding ->
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.onEvent(CleaningEvent.Refresh) },
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(uiState.displayTasks, key = { it.id }) { task ->
                            Box(Modifier.animateItem()) {
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
                        
                        item {
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }

        UnifiedFAB(
            onClick = { viewModel.onEvent(CleaningEvent.ToggleAddDialog(true)) },
            contentDescription = Loc.cleaning_addTask
        )
    }

    if (uiState.showAddDialog) {
        var addTitle by remember { mutableStateOf("") }
        var addDays by remember { mutableStateOf("7") }
        var addRecurring by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CleaningEvent.ToggleAddDialog(false)) },
            title = { Text(Loc.cleaning_addTask) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addTitle,
                        onValueChange = { addTitle = it },
                        label = { Text(Loc.cleaning_taskTitle) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addDays,
                        onValueChange = { if (it.all { c -> c.isDigit() }) addDays = it },
                        label = { Text(Loc.cleaning_everyXDays) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.cleaning_recurring)
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
                    Text(Loc.general_add)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(CleaningEvent.ToggleAddDialog(false)) }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }

    if (uiState.taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(CleaningEvent.RequestDelete(null)) },
            title = { Text(Loc.general_confirmDeleteTitle) },
            text = { Text(Loc.general_confirmDeleteMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(CleaningEvent.ConfirmDelete)
                }) {
                    Text(Loc.general_delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(CleaningEvent.RequestDelete(null)) }) {
                    Text(Loc.general_cancel)
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
            title = { Text(Loc.cleaning_editTask) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text(Loc.cleaning_taskTitle) })
                    OutlinedTextField(value = editDays, onValueChange = { editDays = it }, label = { Text(Loc.cleaning_everyXDays) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.cleaning_recurring)
                        Checkbox(checked = editRecurring, onCheckedChange = { editRecurring = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onEvent(CleaningEvent.SaveTask(editTitle, editDays.toIntOrNull() ?: 1, editRecurring))
                }) { Text(Loc.general_save) }
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

    CompactActionListItem(
        onClick = null, // Disable whole-item click for checkboxes
        headline = task.title,
        supportingText = if (task.isRecurring) Loc.getCleaningFrequency(task.frequencyDays) else Loc.cleaning_oneTime,
        metadataText = if (task.lastDoneTimestamp > 0) "${Loc.cleaning_lastDone}: ${dateFormat.format(Date(task.lastDoneTimestamp))} ${Loc.general_by} $userName" else null,
        leadingContent = {
            if (task.isEnabled) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = task.isChecked,
                        onCheckedChange = onToggle,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(32.dp))
            }
        },
        trailingContent = {
            Box {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                    }
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (task.isEnabled) {
                        DropdownMenuItem(text = { Text(Loc.general_update) }, onClick = { onEdit(); menuExpanded = false })
                        DropdownMenuItem(text = { Text(Loc.cleaning_disable) }, onClick = { onDisable(false); menuExpanded = false })
                        DropdownMenuItem(text = { Text(Loc.general_delete, color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); menuExpanded = false })
                    } else {
                        DropdownMenuItem(text = { Text(Loc.cleaning_restoreTask) }, onClick = { onDisable(true); menuExpanded = false })
                        DropdownMenuItem(text = { Text(Loc.general_delete, color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); menuExpanded = false })
                    }
                }
            }
        },
        isPending = !task.isEnabled
    )
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
            groupSyncManager = GroupSyncManager(context),
            onBack = {}
        )
    }
}
