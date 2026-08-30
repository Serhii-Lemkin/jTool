package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.timecalc.TimeCalculatorScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object TimeCalc : AppProperties(
    AppScreenEnum.TIME_CALC,
    Icons.Default.Calculate,
    { Loc.timecalc_timeCalc }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        TimeCalculatorScreen(onBack = { deps.setScreen(AppScreenEnum.LAUNCHER) })
    }
}
