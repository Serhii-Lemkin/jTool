package com.jtool.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.vaults.VaultDetailsScreen
import com.jtool.android.features.vaults.VaultsManager
import com.jtool.android.features.vaults.VaultsViewModel

object VaultDetails : AppProperties(
    AppScreenEnum.VAULT_DETAILS,
    Icons.Default.FolderSpecial,
    { Loc.Vaults.vaults }
) {
    @Composable
    override fun Content(deps: NavigationDependencies) {
        val context = LocalContext.current
        val viewModel: VaultsViewModel = viewModel(
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

        VaultDetailsScreen(
            dataManager = deps.dataManager,
            settingsManager = deps.settingsManager,
            authManager = deps.authManager,
            isAdmin = deps.isAdmin,
            onBack = { deps.setScreen(AppScreenEnum.VAULTS) },
            onNavigate = { deps.setScreen(it) },
            viewModel = viewModel
        )
    }
}
