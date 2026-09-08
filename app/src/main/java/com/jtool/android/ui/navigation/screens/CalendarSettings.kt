package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.calendar.settings.CalendarSettingsScreen

object CalendarSettings : AppProperties(
    AppScreenEnum.CALENDAR_SETTINGS,
    null,
    { Loc.Launcher.settings }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        CalendarSettingsScreen(
            dataManager = deps.dataManager,
            onBack = { deps.onBack() }
        )
    }
}
