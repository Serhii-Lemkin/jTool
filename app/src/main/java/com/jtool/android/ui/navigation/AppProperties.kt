package com.jtool.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppProperties(
    val id: String,
    val icon: ImageVector? = null,
    val getName: () -> String = { "" }
) {
    @Composable
    abstract fun Content(deps: NavigationDependencies)

    companion object {
        val allApps = listOf(WorkTime, TimeCalc, ShopList, Notes, Links, Cleaning)
        val allScreens = listOf(Launcher, WorkTime, TimeCalc, ShopList, Notes, Links, Cleaning, AdminPanel, Settings, SettingsAccount)
        
        fun fromId(id: String): AppProperties? = allScreens.find { it.id == id }
    }
}
