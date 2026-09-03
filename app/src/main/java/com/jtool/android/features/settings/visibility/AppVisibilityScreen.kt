package com.jtool.android.features.settings.visibility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.models.common.enums.AppScreenEnum

@Composable
fun AppVisibilityScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    viewModel: AppVisibilityViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AppVisibilityViewModel(settingsManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val apps = remember {
        listOf(
            AppScreenEnum.WORK_TIME to Loc.worktime_workTime,
            AppScreenEnum.TIME_CALC to Loc.timecalc_timeCalc,
            AppScreenEnum.SHOP_LIST to Loc.shoplist_shoppingList,
            AppScreenEnum.NOTES to Loc.notes_notes,
            AppScreenEnum.LINKS to Loc.links_links,
            AppScreenEnum.CLEANING to Loc.cleaning_cleaning,
            AppScreenEnum.CALENDAR to Loc.calendar_calendar
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(Loc.settings_layoutType, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (uiState.isGridLayout) Loc.settings_gridLayout else Loc.settings_listLayout, 
                    modifier = Modifier.padding(end = 8.dp), 
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = uiState.isGridLayout,
                    onCheckedChange = { viewModel.onEvent(AppVisibilityEvent.ToggleGridLayout) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            apps.forEach { (id, name) ->
                val isVisible = uiState.visibleApps[id] ?: true
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { viewModel.onEvent(AppVisibilityEvent.ToggleAppVisibility(id, it)) }
                    )
                }
                if (id != apps.last().first) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}
