package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.links.JLinksScreen

object Links : AppProperties(
    AppNames.LINKS,
    Icons.Default.Link,
    { Loc.links }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        JLinksScreen(
            manager = deps.linksManager,
            backupManager = deps.backupManager,
            settingsManager = deps.settingsManager,
            groupSyncManager = deps.groupSyncManager
        )
    }
}
