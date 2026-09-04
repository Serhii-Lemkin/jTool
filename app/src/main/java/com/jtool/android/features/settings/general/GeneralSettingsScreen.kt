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
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.settings.general.components.language_selection_sheet.LanguageSelectionSheet

@Composable
fun GeneralSettingsScreen(
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onThemeToggle: () -> Unit,
    onLangToggle: () -> Unit,
    viewModel: GeneralSettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GeneralSettingsViewModel(settingsManager, dataManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLangSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val languages = remember(context) { Loc.getAvailableLanguages(context) }

    if (showLangSheet) {
        LanguageSelectionSheet(
            languages = languages,
            currentLanguageCode = uiState.language,
            onLanguageSelected = { locale ->
                viewModel.onEvent(GeneralSettingsEvent.SetLanguage(locale))
                onLangToggle()
            },
            onDismiss = { showLangSheet = false }
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
                Text(if (uiState.isDarkMode) Loc.Settings.darkMode else Loc.Settings.lightMode, style = MaterialTheme.typography.bodyLarge)
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
                .clickable { showLangSheet = true }
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
                Text(Loc.Settings.language, style = MaterialTheme.typography.bodyLarge)
            }
            
            Text(
                text = languages.find { it.locale == uiState.language }?.name ?: uiState.language,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
