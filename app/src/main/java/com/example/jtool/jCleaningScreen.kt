package com.example.jtool

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JCleaningScreen(
    manager: JCleaningManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager
) {
    var tasks by remember { mutableStateOf(manager.loadTasks()) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDays by remember { mutableStateOf("7") }
    var isRecurring by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()
    val syncUri = settingsManager.getSyncUri()

    fun triggerSync() {
        if (syncUri != null) {
            scope.launch {
                backupManager.syncPushCleaning(syncUri)
            }
        }
    }

    fun updateTasks(newList: List<CleaningTask>) {
        tasks = newList
        manager.saveTasks(newList)
        triggerSync()
    }

    // Auto-pull and Reset Logic
    LaunchedEffect(Unit) {
        if (syncUri != null) {
            backupManager.syncPullCleaning(syncUri)
        }
        val currentTasks = manager.loadTasks()
        val now = System.currentTimeMillis()
        val updated = currentTasks.map { task ->
            if (task.isChecked && task.isRecurring && task.lastDoneTimestamp > 0) {
                val diffDays = (now - task.lastDoneTimestamp) / (24 * 60 * 60 * 1000)
                if (diffDays >= task.frequencyDays) {
                    task.copy(isChecked = false)
                } else task
            } else task
        }
        updateTasks(updated)
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
        Text(
            text = Loc.jCleaning,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sortedTasks = tasks.sortedWith(
                compareBy<CleaningTask> { it.isChecked }
                    .thenByDescending { it.isEnabled }
                    .thenBy { it.title }
            )
            items(sortedTasks, key = { it.id }) { task ->
                CleaningTaskRow(
                    task = task,
                    onToggle = { checked ->
                        val updated = tasks.map {
                            if (it.id == task.id) {
                                it.copy(
                                    isChecked = checked,
                                    lastDoneTimestamp = if (checked) System.currentTimeMillis() else it.lastDoneTimestamp,
                                    lastUpdatedBy = settingsManager.getUserName()
                                )
                            } else it
                        }
                        updateTasks(updated)
                    },
                    onDelete = {
                        updateTasks(tasks.filter { it.id != task.id })
                    },
                    onEnabledToggle = { enabled ->
                        val updated = tasks.map {
                            if (it.id == task.id) it.copy(isEnabled = enabled) else it
                        }
                        updateTasks(updated)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            val newTask = CleaningTask(
                                id = System.currentTimeMillis(),
                                title = newTaskTitle.trim(),
                                frequencyDays = newTaskDays.toIntOrNull() ?: 1,
                                isRecurring = isRecurring,
                                lastUpdatedBy = settingsManager.getUserName()
                            )
                            updateTasks(tasks + newTask)
                            newTaskTitle = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newTaskTitle.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.addTask)
                }
            }
        }
    }
}

@Composable
fun CleaningTaskRow(
    task: CleaningTask,
    onToggle: (Boolean) -> Unit,
    onEnabledToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val currentLocale = remember(Loc.lang) { if (Loc.lang == "uk") Locale.forLanguageTag("uk") else Locale.ENGLISH }
    val dateFormat = remember(currentLocale) { SimpleDateFormat("MMM dd, HH:mm", currentLocale) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isEnabled) MaterialTheme.colorScheme.surfaceVariant 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.isChecked,
                    onCheckedChange = onToggle,
                    enabled = task.isEnabled
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (task.isEnabled) MaterialTheme.colorScheme.onSurface 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = if (task.isRecurring) Loc.everyDays.format(task.frequencyDays) else Loc.oneTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Switch(
                    checked = task.isEnabled,
                    onCheckedChange = onEnabledToggle,
                    modifier = Modifier.scale(0.7f)
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
            
            if (task.lastDoneTimestamp > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${Loc.lastDone}: ${dateFormat.format(Date(task.lastDoneTimestamp))}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${Loc.by} ${task.lastUpdatedBy}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
            settingsManager = SettingsManager(context)
        )
    }
}
