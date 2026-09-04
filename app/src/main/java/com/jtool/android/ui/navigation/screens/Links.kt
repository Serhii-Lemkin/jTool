package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.links.LinksScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object Links : AppProperties(
    AppScreenEnum.LINKS,
    Icons.Default.Link,
    { Loc.Links.links }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        LinksScreen(
            manager = deps.linksManager,
            backupManager = deps.backupManager,
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            onBack = { deps.setScreen(AppScreenEnum.LAUNCHER) }
        )
    }
}
