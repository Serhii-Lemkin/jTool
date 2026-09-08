package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.tracker.TrackerListScreen
import com.jtool.android.features.tracker.TrackerViewModel

object Trendify : AppProperties(
    id = AppScreenEnum.TRENDIFY,
    icon = Icons.Default.ShowChart,
    getName = { Loc.Tracker.trendify }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val trackerViewModel: TrackerViewModel = viewModel(
            viewModelStoreOwner = LocalContext.current as ComponentActivity,
            factory = viewModelFactory {
                initializer {
                    TrackerViewModel(deps.dataManager)
                }
            }
        )

        TrackerListScreen(
            dataManager = deps.dataManager,
            currentGroupId = deps.currentGroupId,
            onBack = deps.onBack,
            onNavigate = deps.setScreen,
            viewModel = trackerViewModel
        )
    }
}
