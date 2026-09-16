package com.jtool.android.features.settings.visibility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
            AppScreenEnum.WORK_TIME to Loc.Workday.workTime,
            AppScreenEnum.TIME_CALC to Loc.TimeCalc.timeCalc,
            AppScreenEnum.SHOP_LIST to Loc.ShopList.shoppingList,
            AppScreenEnum.NOTES to Loc.Notes.notes,
            AppScreenEnum.LINKS to Loc.Links.links,
            AppScreenEnum.CLEANING to Loc.Cleaning.cleaning,
            AppScreenEnum.CALENDAR to Loc.Calendar.calendar,
            AppScreenEnum.TRENDIFY to Loc.Tracker.trendify
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        SectionHeader(title = Loc.Settings.layoutType, icon = Icons.Default.Palette)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (uiState.isGridLayout) Loc.Settings.gridLayout else Loc.Settings.listLayout, 
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = Loc.Settings.layoutDescription, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.isGridLayout,
                onCheckedChange = { viewModel.onEvent(AppVisibilityEvent.ToggleGridLayout) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(title = Loc.Settings.visibilityTitle, icon = Icons.Default.Visibility)

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

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title, 
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}
