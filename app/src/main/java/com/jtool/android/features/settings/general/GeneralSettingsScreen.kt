package com.jtool.android.features.settings.general

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager

@Composable
fun GeneralSettingsScreen(
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
    onThemeToggle: () -> Unit,
    onLangToggle: () -> Unit,
    viewModel: GeneralSettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GeneralSettingsViewModel(settingsManager, groupSyncManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var langMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val languages = remember(context) { Loc.getAvailableLanguages(context) }

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
                .clickable { 
                    viewModel.onEvent(GeneralSettingsEvent.ToggleTheme)
                    onThemeToggle()
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (uiState.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(if (uiState.isDarkMode) Loc.settings_darkMode else Loc.settings_lightMode, style = MaterialTheme.typography.bodyLarge)
            }
            Switch(
                checked = uiState.isDarkMode, 
                onCheckedChange = { 
                    viewModel.onEvent(GeneralSettingsEvent.ToggleTheme)
                    onThemeToggle()
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { langMenuExpanded = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(Loc.settings_language, style = MaterialTheme.typography.bodyLarge)
            }
            
            Box {
                Text(
                    text = languages.find { it.locale == uiState.language }?.name ?: uiState.language,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                DropdownMenu(
                    expanded = langMenuExpanded,
                    onDismissRequest = { langMenuExpanded = false }
                ) {
                    languages.forEach { langMeta ->
                        DropdownMenuItem(
                            text = { Text(langMeta.name) },
                            onClick = {
                                viewModel.onEvent(GeneralSettingsEvent.SetLanguage(langMeta.locale))
                                onLangToggle()
                                langMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
