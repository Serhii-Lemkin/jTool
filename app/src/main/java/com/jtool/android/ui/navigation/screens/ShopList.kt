package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.shopping.ShoppingListScreen
import com.jtool.android.core.models.common.enums.AppScreenEnum

object ShopList : AppProperties(
    AppScreenEnum.SHOP_LIST,
    Icons.Default.ShoppingCart,
    { Loc.ShopList.shoppingList }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        ShoppingListScreen(
            manager = deps.shoppingListManager,
            settingsManager = deps.settingsManager,
            dataManager = deps.dataManager,
            onBack = { deps.setScreen(AppScreenEnum.LAUNCHER) }
        )
    }
}
