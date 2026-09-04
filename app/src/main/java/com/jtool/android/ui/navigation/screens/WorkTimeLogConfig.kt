package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.workday.logconfig.LogConfigScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.workday.logconfig.LogConfigViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

object WorkTimeLogConfig : AppProperties(
    AppScreenEnum.WORK_TIME_LOG_CONFIG,
    null,
    { Loc.Workday.manual }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val viewModel: LogConfigViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    LogConfigViewModel(deps.settingsManager, deps.dataManager)
                }
            }
        )
        LogConfigScreen(
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            onBack = { deps.onBack() },
            viewModel = viewModel
        )
    }
}
