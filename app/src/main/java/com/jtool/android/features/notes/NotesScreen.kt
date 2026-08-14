package com.jtool.android.features.notes

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.features.notes.edit.NoteEditScreen
import com.jtool.android.features.notes.list.NoteListScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
fun NotesScreen(
    manager: NotesManager,
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
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.currentScreen == NotesScreenType.EDIT) {
        BackHandler { viewModel.onEvent(NotesEvent.NavigateBack) }
        NoteEditScreen(uiState, viewModel)
    } else {
        NoteListScreen(uiState, viewModel)
    }

    if (uiState.noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(NotesEvent.RequestDelete(null)) },
            title = { Text(Loc.deleteNoteTitle) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(NotesEvent.ConfirmDelete) }) {
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
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun NotesPreview() {
    val context = LocalContext.current
    MaterialTheme {
        NotesScreen(
            manager = NotesManager(context),
            backupManager = BackupManager(context),
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context)
        )
    }
}
