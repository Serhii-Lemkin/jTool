package com.jtool.android.features.workday.main_settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.EventColorPickerSheet
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getGoogleCalendarColorPalette
import java.time.DayOfWeek
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkdaySettingsMainScreen(
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit
) {
    var showDayPicker by remember { mutableStateOf(false) }
    
    val options = remember { settingsManager.getWorktimeOptions() }
    var startDay by remember { mutableStateOf(try { DayOfWeek.valueOf(options.weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }) }
    
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showDayPicker) {
        ModalBottomSheet(
            onDismissRequest = { showDayPicker = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = Loc.Workday.weeklyStartDay, 
                    style = MaterialTheme.typography.titleLarge, 
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                DayOfWeek.entries.forEach { day ->
                    val isSelected = startDay == day
                    ActionListItem(
                        headline = Loc.getDayOfWeekName(day),
                        trailingContent = if (isSelected) {
                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = { 
                            startDay = day
                            val newOptions = settingsManager.getWorktimeOptions().copy(weeklyStartDay = day.name)
                            settingsManager.saveWorktimeOptions(newOptions)
                            scope.launch { dataManager.pushUserSettings() }
                            showDayPicker = false
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            GenericTitle(
                title = Loc.Launcher.settings,
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ActionListItem(
                headline = Loc.Workday.weeklyStartDay,
                supportingText = Loc.getDayOfWeekName(startDay),
                leadingContent = { Icon(Icons.Default.CalendarToday, null) },
                onClick = { showDayPicker = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ActionListItem(
                headline = Loc.Workday.setWorkHours,
                supportingText = Loc.Workday.setWorkHoursDescription,
                leadingContent = { Icon(Icons.Default.Timer, null) },
                onClick = { onNavigate(AppScreenEnum.WORK_TIME_LOG_CONFIG) },
                supportingMaxLines = Int.MAX_VALUE
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ActionListItem(
                headline = Loc.Workday.setSchedule,
                supportingText = Loc.Workday.setScheduleDescription,
                leadingContent = { Icon(Icons.Default.DateRange, null) },
                onClick = { onNavigate(AppScreenEnum.WORK_TIME_SCHEDULE) },
                supportingMaxLines = Int.MAX_VALUE
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            var showWorkShiftColorPicker by remember { mutableStateOf(false) }
            var currentWorkShiftColorKey by remember { mutableStateOf(settingsManager.getWorkShiftColorKey()) }
            val colorPalette = getGoogleCalendarColorPalette()
            val currentColorOption = colorPalette.find { it.key == currentWorkShiftColorKey } ?: colorPalette.first()

            ActionListItem(
                headline = Loc.Calendar.workShiftColor,
                supportingText = Loc.Calendar.workShiftColorSubtitle,
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(currentColorOption.hexColor)
                    )
                },
                onClick = { showWorkShiftColorPicker = true },
                supportingMaxLines = Int.MAX_VALUE
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            if (showWorkShiftColorPicker) {
                EventColorPickerSheet(
                    selectedColorKey = currentWorkShiftColorKey,
                    onSelectColor = { colorOption ->
                        currentWorkShiftColorKey = colorOption.key
                        settingsManager.setWorkShiftColorKey(colorOption.key)
                        scope.launch { dataManager.pushUserSettings() }
                    },
                    onDismiss = { showWorkShiftColorPicker = false }
                )
            }

            Spacer(Modifier.weight(1f))
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            var showDeletePrompt by remember { mutableStateOf(false) }

            if (showDeletePrompt) {
                ModalBottomSheet(
                    onDismissRequest = { showDeletePrompt = false },
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                                        Text(
                    text = Loc.Workday.resetConfirmTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Loc.Workday.resetConfirmMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        val freshOptions = com.jtool.android.core.models.workday.WorktimeOptions(
                            trackingPreference = WorkTrackingPreferenceEnum.UNSET
                        )
                        settingsManager.saveWorktimeOptions(freshOptions)
                        scope.launch { 
                            val uid = settingsManager.getUserId()
                            dataManager.clearWorkdayOptions(uid)
                        }
                        showDeletePrompt = false
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Loc.General.reset)
                }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = { showDeletePrompt = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Loc.General.cancel)
                        }
                    }
                }
            }

            ActionListItem(
                headline = Loc.Workday.deleteAllData,
                supportingText = Loc.Workday.resetToDefaults,
                leadingContent = { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { showDeletePrompt = true }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
