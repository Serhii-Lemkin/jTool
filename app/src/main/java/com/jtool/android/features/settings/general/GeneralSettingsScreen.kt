package com.jtool.android.features.settings.general

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.SettingsLoc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.settings.general.components.language_selection_sheet.LanguageSelectionSheet
import com.jtool.android.features.settings.general.components.theme_customization_sheet.ThemeCustomizationSheet
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
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
    var showColorSheet by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showDateFormatPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val languages = remember(context) { Loc.getAvailableLanguages(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    if (showColorSheet) {
        ThemeCustomizationSheet(
            isDarkMode = uiState.isDarkMode,
            currentPrimaryHex = if (uiState.isDarkMode) uiState.darkPrimary else uiState.lightPrimary,
            currentBgHex = if (uiState.isDarkMode) uiState.darkBg else uiState.lightBg,
            onSave = { primary, bg ->
                viewModel.onEvent(GeneralSettingsEvent.SetThemeColors(primary, bg, uiState.isDarkMode))
                onThemeToggle()
            },
            onDismiss = { showColorSheet = false }
        )
    }

    if (showDayPicker) {
        ModalBottomSheet(
            onDismissRequest = { showDayPicker = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = Loc.Workday.weeklyStartDay, 
                    style = MaterialTheme.typography.titleLarge, 
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                DayOfWeek.entries.forEach { day ->
                    val isSelected = uiState.weeklyStartDay == day
                    ActionListItem(
                        headline = Loc.getDayOfWeekName(day),
                        trailingContent = if (isSelected) {
                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = { 
                            viewModel.onEvent(GeneralSettingsEvent.SetWeeklyStartDay(day))
                            showDayPicker = false
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (showDateFormatPicker) {
        ModalBottomSheet(
            onDismissRequest = { showDateFormatPicker = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = SettingsLoc.selectDateFormat, 
                    style = MaterialTheme.typography.titleLarge, 
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val formats = listOf("DD/MM/YYYY", "MM/DD/YYYY", "YYYY/MM/DD")
                formats.forEach { fmt ->
                    val isSelected = uiState.dateFormat == fmt
                    ActionListItem(
                        headline = fmt,
                        supportingText = when (fmt) {
                            "DD/MM/YYYY" -> "31/12/2026 (${Loc.s("settings_dayMonthYear", "Day/Month/Year")})"
                            "MM/DD/YYYY" -> "12/31/2026 (${Loc.s("settings_monthDayYear", "Month/Day/Year")})"
                            else -> "2026/12/31 (${Loc.s("settings_yearMonthDay", "Year/Month/Day")})"
                        },
                        trailingContent = if (isSelected) {
                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = { 
                            viewModel.onEvent(GeneralSettingsEvent.SetDateFormat(fmt))
                            showDateFormatPicker = false
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showColorSheet = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(SettingsLoc.customizeTheme, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = Loc.s("general_configure", "Configure"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDayPicker = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(Loc.Workday.weeklyStartDay, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = Loc.getDayOfWeekName(uiState.weeklyStartDay),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDateFormatPicker = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(SettingsLoc.dateFormat, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = uiState.dateFormat,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
