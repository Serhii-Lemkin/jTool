package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

import com.jtool.android.core.models.common.enums.AppScreenEnum

sealed class AppProperties(
    val id: AppScreenEnum,
    val icon: ImageVector? = null,
    val getName: () -> String = { "" }
) {
    @Composable
    abstract fun Content(deps: NavigationDependencies)

    companion object {
        val allApps = listOf(WorkTime, Calendar, ShopList, Notes, Cleaning, Links, Trendify, TimeCalc)
        val allScreens = listOf(Launcher, WorkTime, WorkTimeSettingsMain, WorkTimeSchedule, WorkTimeLogConfig, TimeCalc, ShopList, Notes, Links, Cleaning, Trendify, TrendifyDetails, Calendar, CalendarDay, CalendarEventDetail, CalendarSettings, CalendarAddEdit, AdminPanel, Settings, SettingsAccount, SupportHub)
        
        fun fromId(id: AppScreenEnum): AppProperties? = allScreens.find { it.id == id }
    }
}
