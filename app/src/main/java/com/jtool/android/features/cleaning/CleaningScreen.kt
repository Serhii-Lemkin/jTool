package com.jtool.android.features.cleaning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.feature.cleaning.CleaningTask
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.components.layout.CompactActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.features.cleaning.components.cleaning_task_delete_confirm_sheet.CleaningTaskDeleteConfirmSheet
import com.jtool.android.features.cleaning.components.cleaning_task_edit_sheet.CleaningTaskEditSheet
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
    dataManager: DataManager,
    onBack: () -> Unit,
    viewModel: CleaningViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CleaningViewModel(manager, settingsManager, dataManager)
            }
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    BaseScreen(
        title = if (uiState.showDisabledView) Loc.Cleaning.showDisabled else Loc.Cleaning.cleaning,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(CleaningEvent.Refresh) },
        actions = {
            TitleActionIcon(
                icon = if (uiState.showDisabledView) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                onClick = { viewModel.onEvent(CleaningEvent.ToggleDisabledView(!uiState.showDisabledView)) },
                tint = if (uiState.showDisabledView) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
            com.jtool.android.core.components.layout.TitleDropdownMenu {
                com.jtool.android.core.components.layout.ActionListItem(
                    headline = com.jtool.android.core.localization.domains.GeneralLoc.learnMore,
                    supportingText = Loc.s("cleaning_learnMoreSubtitle", "View online guide and feature documentation"),
                    leadingContent = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "cleaning") }
                )
            }
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { viewModel.onEvent(CleaningEvent.ToggleAddDialog(true)) },
                contentDescription = Loc.Cleaning.addTask
            )
        }
    ) { _ ->
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

    if (uiState.showAddDialog) {
        var addTitle by remember { mutableStateOf("") }
        var addDays by remember { mutableStateOf("7") }
        var addRecurring by remember { mutableStateOf(true) }

        CleaningTaskEditSheet(
            title = addTitle,
            days = addDays,
            isRecurring = addRecurring,
            isEdit = false,
            onTitleChange = { addTitle = it },
            onDaysChange = { addDays = it },
            onRecurringChange = { addRecurring = it },
            onDismiss = { viewModel.onEvent(CleaningEvent.ToggleAddDialog(false)) },
            onConfirm = { 
                viewModel.onEvent(CleaningEvent.AddTask(addTitle, addDays.toIntOrNull() ?: 1, addRecurring))
                viewModel.onEvent(CleaningEvent.ToggleAddDialog(false))
            }
        )
    }

    if (uiState.taskToDelete != null) {
        CleaningTaskDeleteConfirmSheet(
            onDismiss = { viewModel.onEvent(CleaningEvent.RequestDelete(null)) },
            onConfirm = { viewModel.onEvent(CleaningEvent.ConfirmDelete) }
        )
    }

    if (uiState.editingTask != null) {
        var editTitle by remember { mutableStateOf(uiState.editingTask!!.title) }
        var editDays by remember { mutableStateOf(uiState.editingTask!!.frequencyDays.toString()) }
        var editRecurring by remember { mutableStateOf(uiState.editingTask!!.isRecurring) }

        CleaningTaskEditSheet(
            title = editTitle,
            days = editDays,
            isRecurring = editRecurring,
            isEdit = true,
            onTitleChange = { editTitle = it },
            onDaysChange = { editDays = it },
            onRecurringChange = { editRecurring = it },
            onDismiss = { viewModel.onEvent(CleaningEvent.RequestEdit(null)) },
            onConfirm = { 
                viewModel.onEvent(CleaningEvent.SaveTask(editTitle, editDays.toIntOrNull() ?: 1, editRecurring))
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
        onClick = null,
        headline = task.title,
        supportingText = if (task.isRecurring) Loc.getCleaningFrequency(task.frequencyDays) else Loc.Cleaning.oneTime,
        metadataText = if (task.lastDoneTimestamp > 0) "${Loc.Cleaning.lastDone}: ${dateFormat.format(Date(task.lastDoneTimestamp))} ${Loc.General.by} $userName" else null,
        leadingContent = {
            if (task.isEnabled) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = task.isChecked,
                        onCheckedChange = onToggle,
                        modifier = Modifier.scale(0.85f).offset(x = (-4).dp)
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
                        DropdownMenuItem(text = { Text(Loc.General.edit) }, onClick = { onEdit(); menuExpanded = false })
                        DropdownMenuItem(text = { Text(Loc.Cleaning.disable) }, onClick = { onDisable(false); menuExpanded = false })
                        DropdownMenuItem(text = { Text(Loc.General.delete, color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); menuExpanded = false })
                    } else {
                        DropdownMenuItem(text = { Text(Loc.Cleaning.restoreTask) }, onClick = { onDisable(true); menuExpanded = false })
                        DropdownMenuItem(text = { Text(Loc.General.delete, color = MaterialTheme.colorScheme.error) }, onClick = { onDelete(); menuExpanded = false })
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
            dataManager = DataManager(context),
            onBack = {}
        )
    }
}
