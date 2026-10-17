package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.vaults.VaultsManager
import com.jtool.android.features.vaults.VaultsViewModel
import com.jtool.android.features.vaults.settings.VaultSettingsScreen
import com.jtool.android.ui.navigation.AppProperties
import com.jtool.android.ui.navigation.NavigationDependencies

object VaultSettings : AppProperties(
    id = AppScreenEnum.VAULT_SETTINGS,
    icon = Icons.Default.Settings,
    getName = { "Vault Settings" }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val context = LocalContext.current
        val vaultsViewModel: VaultsViewModel = viewModel(
            viewModelStoreOwner = LocalContext.current as ComponentActivity,
            factory = viewModelFactory {
                initializer {
                    VaultsViewModel(
                        vaultsManager = VaultsManager(context),
                        settingsManager = deps.settingsManager,
                        dataManager = deps.dataManager,
                        context = context
                    )
                }
            }
        )

        VaultSettingsScreen(
            dataManager = deps.dataManager,
            settingsManager = deps.settingsManager,
            onBack = deps.onBack,
            viewModel = vaultsViewModel
        )
    }
}
