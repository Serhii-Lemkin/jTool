package com.example.jtool.settingsSections

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jtool.Loc
import com.example.jtool.SettingsManager

@Composable
fun AppVisibilityScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apps = remember {
        listOf(
            "WorkTime" to "WorkTime",
            "TimeCalc" to "TimeCalc",
            "ShopList" to "ShopList",
            "Notes" to "Notes",
            "Links" to "Links",
            "Cleaning" to "Cleaning"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Loc.visibilityTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Layout Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(Loc.layoutType, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var isGrid by remember { mutableStateOf(settingsManager.isGridLayout()) }
                    Text(if (isGrid) Loc.gridLayout else Loc.listLayout, modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = isGrid,
                        onCheckedChange = {
                            isGrid = it
                            settingsManager.setGridLayout(it)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                apps.forEach { (id, name) ->
                    var isVisible by remember { mutableStateOf(settingsManager.isAppVisible(id)) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = name, fontWeight = FontWeight.Medium)
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
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}
