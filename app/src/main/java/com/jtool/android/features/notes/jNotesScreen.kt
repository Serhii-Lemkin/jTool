package com.jtool.android.features.notes

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.NoteItem
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
fun JNotesScreen(
    manager: JNotesManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
    viewModel: NotesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                NotesViewModel(manager, settingsManager, groupSyncManager)
            }
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(NotesEvent.RequestDelete(null)) },
            title = { Text(Loc.confirmDeleteTitle) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(NotesEvent.ConfirmDelete)
                }) {
                    Text(Loc.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(NotesEvent.RequestDelete(null)) }) {
                    Text(Loc.cancel)
                }
            }
        )
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
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(uiState.notes.sortedByDescending { it.timestamp }, key = { it.id }) { note ->
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
                                viewModel.onEvent(NotesEvent.RequestDelete(note))
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
                            text = "${Loc.by} ${uiState.userNames[note.lastUpdatedBy] ?: note.lastUpdatedBy}",
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
                value = uiState.newNoteText,
                onValueChange = { viewModel.onEvent(NotesEvent.UpdateNewNoteText(it)) },
                label = { Text(Loc.newNote) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.onEvent(NotesEvent.AddNote)
                },
                enabled = uiState.newNoteText.isNotBlank()
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
