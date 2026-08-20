package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.shopping.ShoppingListScreen

object ShopList : AppProperties(
    AppNames.SHOP_LIST,
    Icons.Default.ShoppingCart,
    { Loc.shoplist_shoppingList }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        ShoppingListScreen(
            manager = deps.shoppingListManager,
            backupManager = deps.backupManager,
            settingsManager = deps.settingsManager,
            groupSyncManager = deps.groupSyncManager,
            onBack = { deps.setScreen(AppNames.LAUNCHER) }
        )
    }
}
