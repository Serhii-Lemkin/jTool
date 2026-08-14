package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.notes.NotesScreen

object Notes : AppProperties(
    AppNames.NOTES,
    Icons.Default.Description,
    { Loc.notes }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        NotesScreen(
            manager = deps.notesManager,
            backupManager = deps.backupManager,
            settingsManager = deps.settingsManager,
            groupSyncManager = deps.groupSyncManager
        )
    }
}
