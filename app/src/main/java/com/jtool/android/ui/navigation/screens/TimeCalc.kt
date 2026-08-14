package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.timecalc.TimeCalculatorScreen

object TimeCalc : AppProperties(
    AppNames.TIME_CALC,
    Icons.Default.Calculate,
    { Loc.timeCalc }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        TimeCalculatorScreen()
    }
}
