package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.workday.main_settings.WorkdaySettingsMainScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object WorkTimeSettingsMain : AppProperties(
    AppScreenEnum.WORK_TIME_SETTINGS_MAIN,
    null,
    { Loc.Launcher.settings }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        WorkdaySettingsMainScreen(
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            onBack = { deps.onBack() },
            onNavigate = { deps.setScreen(it) }
        )
    }
}
