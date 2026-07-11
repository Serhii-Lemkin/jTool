package com.example.jtool

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JCleaningScreen(
    manager: JCleaningManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager
) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(manager.loadTasks()) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDays by remember { mutableStateOf("7") }
    var isRecurring by remember { mutableStateOf(true) }
    var showDisabledView by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val currentGroupId = remember { settingsManager.getCurrentGroupId() }
    val userNames by groupSyncManager.userNamesFlow().collectAsState(initial = emptyMap())

    var editingTask by remember { mutableStateOf<CleaningTask?>(null) }

    fun triggerSync() {
        settingsManager.setSyncPending(true)
        scope.launch {
            groupSyncManager.pushGroupData()
        }
    }

    fun updateTasks(newList: List<CleaningTask>) {
        tasks = newList
        manager.saveTasks(newList)
        triggerSync()
    }

    // Real-time Firebase Sync
    LaunchedEffect(currentGroupId) {
        if (currentGroupId != null) {
            groupSyncManager.groupDataFlow(currentGroupId).collect {
                tasks = manager.loadTasks()
            }
        }
    }

    // Auto-pull and Reset Logic
    LaunchedEffect(Unit) {
        tasks = manager.loadTasks()
        while (true) {
            val currentTasks = manager.loadTasks()
            val now = System.currentTimeMillis()
            val updated = currentTasks.map { task ->
                if (task.isEnabled && task.isChecked && task.isRecurring && task.lastDoneTimestamp > 0) {
                    val diffDays = (now - task.lastDoneTimestamp) / (24 * 60 * 60 * 1000)
                    if (diffDays >= task.frequencyDays) {
                        task.copy(isChecked = false)
                    } else task
                } else task
            }
            tasks = updated
            manager.saveTasks(updated)
            delay(10000L) // Default 10s check
        }
    }

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
                text = if (showDisabledView) Loc.showDisabled else Loc.cleaning,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            
            var menuExpanded by remember { mutableStateOf(false) }
            Box(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (showDisabledView) Loc.activeTasks else Loc.showDisabled) },
                        onClick = { 
                            showDisabledView = !showDisabledView
                            menuExpanded = false 
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
            val displayTasks = if (showDisabledView) {
                tasks.filter { !it.isEnabled }
            } else {
                tasks.filter { it.isEnabled }.sortedWith(
                    compareBy<CleaningTask> { it.isChecked }
                        .thenBy { it.title }
                )
            }

            items(displayTasks, key = { it.id }) { task ->
                CleaningTaskRow(
                    task = task,
                    onToggle = { checked ->
                        val updated = tasks.map {
                            if (it.id == task.id) {
                                it.copy(
                                    isChecked = checked,
                                    lastDoneTimestamp = if (checked) System.currentTimeMillis() else it.lastDoneTimestamp,
                                    lastUpdatedBy = settingsManager.getAppUserId(),
                                    timestamp = System.currentTimeMillis()
                                )
                            } else it
                        }
                        updateTasks(updated)
                    },
                    onDelete = { updateTasks(tasks.filter { it.id != task.id }) },
                    onDisable = { isEnabled ->
                        val updated = tasks.map {
                            if (it.id == task.id) it.copy(isEnabled = isEnabled, isChecked = false) else it
                        }
                        updateTasks(updated)
                    },
                    onEdit = { editingTask = task },
                    userName = userNames[task.lastUpdatedBy] ?: task.lastUpdatedBy
                )
            }
        }

        if (!showDisabledView) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text(Loc.taskTitle) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newTaskDays,
                            onValueChange = { if (it.all { c -> c.isDigit() }) newTaskDays = it },
                            label = { Text(Loc.everyXDays) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Loc.recurring, fontSize = 12.sp)
                            Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            val newTask = CleaningTask(
                                id = System.currentTimeMillis(),
                                title = newTaskTitle.trim(),
                                frequencyDays = newTaskDays.toIntOrNull() ?: 1,
                                isRecurring = isRecurring,
                                lastUpdatedBy = settingsManager.getAppUserId()
                            )
                            updateTasks(tasks + newTask)
                            newTaskTitle = ""
                        }
                    },
                    enabled = newTaskTitle.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    }

    if (editingTask != null) {
        var editTitle by remember { mutableStateOf(editingTask!!.title) }
        var editDays by remember { mutableStateOf(editingTask!!.frequencyDays.toString()) }
        var editRecurring by remember { mutableStateOf(editingTask!!.isRecurring) }

        AlertDialog(
            onDismissRequest = { editingTask = null },
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
                    val updated = tasks.map {
                        if (it.id == editingTask!!.id) it.copy(
                            title = editTitle, 
                            frequencyDays = editDays.toIntOrNull() ?: it.frequencyDays,
                            isRecurring = editRecurring,
                            timestamp = System.currentTimeMillis()
                        ) else it
                    }
                    updateTasks(updated)
                    editingTask = null
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
        JCleaningScreen(
            manager = JCleaningManager(context),
            backupManager = BackupManager(context),
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context)
        )
    }
}
