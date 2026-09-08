package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.tracker.TrackerDetailsScreen
import com.jtool.android.features.tracker.TrackerViewModel

object TrendifyDetails : AppProperties(AppScreenEnum.TRENDIFY_DETAILS) {
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

        TrackerDetailsScreen(
            dataManager = deps.dataManager,
            authManager = deps.authManager,
            settingsManager = deps.settingsManager,
            onBack = deps.onBack,
            viewModel = trackerViewModel
        )
    }
}
