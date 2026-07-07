package com.example.jtool

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(manager.loadNotes()) }
    var newNoteText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val syncUri = remember { settingsManager.getSyncUri()?.let { Uri.parse(it) } }

    fun triggerSync() {
        if (syncUri != null) {
            scope.launch {
                val success = backupManager.syncPushNotes(syncUri.toString())
                settingsManager.setSyncPending(!success)
            }
        }
    }

    fun updateNotes(newList: List<NoteItem>) {
        notes = newList
        manager.saveNotes(newList)
        triggerSync()
    }

    // Aggressive Sync: Observer + Polling
    LaunchedEffect(syncUri, lifecycleOwner) {
        if (syncUri != null) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        scope.launch {
                            if (backupManager.syncPullNotes(syncUri.toString())) {
                                notes = manager.loadNotes()
                            }
                        }
                    }
                }
                
                try {
                    context.contentResolver.registerContentObserver(syncUri, false, observer)
                    while (true) {
                        if (backupManager.syncPullNotes(syncUri.toString())) {
                            notes = manager.loadNotes()
                        }
                        if (settingsManager.isSyncPending()) {
                            val success = backupManager.syncPushNotes(syncUri.toString())
                            settingsManager.setSyncPending(!success)
                        }
                        delay(10000)
                    }
                } catch (_: Exception) {
                } finally {
                    context.contentResolver.unregisterContentObserver(observer)
                }
            }
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
        Text(
            text = Loc.jNotes,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes.sortedByDescending { it.timestamp }, key = { it.id }) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Text(
                            text = "${Loc.by} ${note.lastUpdatedBy}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.End)
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
                            lastUpdatedBy = settingsManager.getUserName()
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
