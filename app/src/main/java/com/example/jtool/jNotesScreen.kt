package com.example.jtool

import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun JNotesScreen(
    manager: JNotesManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager // Added this
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(manager.loadNotes()) }
    var newNoteText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val userNames by groupSyncManager.userNamesFlow().collectAsState(initial = emptyMap())
    
    val currentGroupId = remember { settingsManager.getCurrentGroupId() }

    fun triggerSync() {
        settingsManager.setSyncPending(true)
        scope.launch {
            groupSyncManager.pushGroupData()
        }
    }

    fun updateNotes(newList: List<NoteItem>) {
        notes = newList
        manager.saveNotes(newList)
        triggerSync()
    }

    // Real-time Firebase Sync
    LaunchedEffect(currentGroupId) {
        if (currentGroupId != null) {
            groupSyncManager.groupDataFlow(currentGroupId).collect {
                notes = manager.loadNotes()
            }
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        notes = manager.loadNotes()
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
            text = Loc.notes,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(notes.sortedByDescending { it.timestamp }, key = { it.id }) { note ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = note.content,
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp
                            )
                            IconButton(onClick = {
                                updateNotes(notes.filter { it.id != note.id })
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "${Loc.by} ${userNames[note.lastUpdatedBy] ?: note.lastUpdatedBy}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.End).padding(end = 12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newNoteText,
                onValueChange = { newNoteText = it },
                label = { Text(Loc.newNote) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newNoteText.isNotBlank()) {
                        val newNote = NoteItem(
                            id = System.currentTimeMillis(),
                            content = newNoteText.trim(),
                            lastUpdatedBy = settingsManager.getAppUserId() // Use ID
                        )
                        updateNotes(notes + newNote)
                        newNoteText = ""
                    }
                },
                enabled = newNoteText.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun NotesPreview() {
    val context = LocalContext.current
    MaterialTheme {
        JNotesScreen(
            manager = JNotesManager(context),
            backupManager = BackupManager(context),
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context)
        )
    }
}
