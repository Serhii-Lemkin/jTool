package com.jtool.android.features.notes

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.models.feature.notes.enums.NotesScreenTypeEnum
import com.jtool.android.features.notes.edit.NoteEditScreen
import com.jtool.android.features.notes.list.NoteListScreen
import com.jtool.android.features.notes.components.note_delete_confirm_sheet.NoteDeleteConfirmSheet
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
fun NotesScreen(
    manager: NotesManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onBack: () -> Unit,
    viewModel: NotesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                NotesViewModel(manager, settingsManager, dataManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.currentScreen == NotesScreenTypeEnum.EDIT) {
        BackHandler { viewModel.onEvent(NotesEvent.NavigateBack) }
        NoteEditScreen(uiState, viewModel)
    } else {
        NoteListScreen(uiState, viewModel, onBack)
    }

    if (uiState.noteToDelete != null) {
        NoteDeleteConfirmSheet(
            onDismiss = { viewModel.onEvent(NotesEvent.RequestDelete(null)) },
            onConfirm = { viewModel.onEvent(NotesEvent.ConfirmDelete) }
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
            dataManager = DataManager(context),
            onBack = {}
        )
    }
}
