package com.example.jtool.settingsSections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jtool.Loc
import com.example.jtool.SettingsManager

@Composable
fun AppVisibilityScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val apps = remember {
        listOf(
            "WorkTime" to Loc.workTime,
            "TimeCalc" to Loc.timeCalc,
            "ShopList" to Loc.shoppingList,
            "Notes" to Loc.notes,
            "Links" to Loc.links,
            "Cleaning" to Loc.cleaning
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Layout Toggle Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(Loc.layoutType, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                var isGrid by remember { mutableStateOf(settingsManager.isGridLayout()) }
                Text(if (isGrid) Loc.gridLayout else Loc.listLayout, modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = isGrid,
                    onCheckedChange = {
                        isGrid = it
                        settingsManager.setGridLayout(it)
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        // Apps Section
        Column(modifier = Modifier.fillMaxWidth()) {
            apps.forEach { (id, name) ->
                var isVisible by remember { mutableStateOf(settingsManager.isAppVisible(id)) }
                
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
                        onCheckedChange = {
                            isVisible = it
                            settingsManager.setAppVisible(id, it)
                        }
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
