package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.notes.NotesScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object Notes : AppProperties(
    AppScreenEnum.NOTES,
    Icons.Default.Description,
    { Loc.notes_notes }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        NotesScreen(
            manager = deps.notesManager,
            backupManager = deps.backupManager,
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            onBack = { deps.setScreen(AppScreenEnum.LAUNCHER) }
        )
    }
}
