package com.jtool.android.features.workday.main_settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import com.jtool.android.features.workday.tracker.TrackingPreferenceBottomSheet
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import java.time.DayOfWeek
import java.time.LocalDate
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
    var showPreferencePicker by remember { mutableStateOf(false) }
    var showOverrideConfirm by remember { mutableStateOf<WorkTrackingPreferenceEnum?>(null) }
    
    val options = remember { settingsManager.getWorktimeOptions() }
    var startDay by remember { mutableStateOf(try { DayOfWeek.valueOf(options.weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }) }
    var trackingPreference by remember { mutableStateOf(options.trackingPreference ?: WorkTrackingPreferenceEnum.UNSET) }
    
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun applyPreference(pref: WorkTrackingPreferenceEnum, overrideShifts: Boolean) {
        trackingPreference = pref
        val newOptions = settingsManager.getWorktimeOptions().copy(trackingPreference = pref)
        settingsManager.saveWorktimeOptions(newOptions)
        scope.launch { 
            if (overrideShifts) {
                val userId = settingsManager.getUserId()
                val now = LocalDate.now()
                // Override from today and on.
                dataManager.clearPrivateCalendarShifts(userId, now.toString(), now.plusYears(1).toString())
            }
            dataManager.pushUserSettings() 
        }
        showPreferencePicker = false
        showOverrideConfirm = null
    }

    if (showOverrideConfirm != null) {
        ModalBottomSheet(
            onDismissRequest = { showOverrideConfirm = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Override Existing Shifts?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Switching tracking methods. Do you want to clear your current scheduled shifts to avoid duplicates?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { applyPreference(showOverrideConfirm!!, true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear & Switch")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { applyPreference(showOverrideConfirm!!, false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Keep Existing")
                }
            }
        }
    }

    if (showPreferencePicker) {
        TrackingPreferenceBottomSheet(
            onDismiss = { showPreferencePicker = false },
            onPreferenceSelected = { pref ->
                if (trackingPreference != WorkTrackingPreferenceEnum.UNSET && trackingPreference != pref) {
                    showOverrideConfirm = pref
                } else {
                    applyPreference(pref, false)
                }
            },
            selectedPreference = trackingPreference
        )
    }

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
                    text = Loc.worktime_weeklyStartDay, 
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
                title = Loc.launcher_settings,
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
                headline = Loc.worktime_trackingMethod,
                supportingText = when (trackingPreference) {
                    WorkTrackingPreferenceEnum.LOG_HOURS -> Loc.worktime_manual
                    WorkTrackingPreferenceEnum.SCHEDULE -> Loc.worktime_weekly
                    WorkTrackingPreferenceEnum.UNSET -> Loc.worktime_unsetPreference
                },
                leadingContent = { Icon(Icons.Default.Tune, null) },
                onClick = { showPreferencePicker = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ActionListItem(
                headline = Loc.worktime_weeklyStartDay,
                supportingText = Loc.getDayOfWeekName(startDay),
                leadingContent = { Icon(Icons.Default.CalendarToday, null) },
                onClick = { showDayPicker = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            if (trackingPreference == WorkTrackingPreferenceEnum.LOG_HOURS || trackingPreference == WorkTrackingPreferenceEnum.UNSET) {
                ActionListItem(
                    headline = Loc.worktime_setWorkHours,
                    supportingText = "Configure daily duration goals",
                    leadingContent = { Icon(Icons.Default.Timer, null) },
                    onClick = { onNavigate(AppScreenEnum.WORK_TIME_LOG_CONFIG) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            
            if (trackingPreference == WorkTrackingPreferenceEnum.SCHEDULE || trackingPreference == WorkTrackingPreferenceEnum.UNSET) {
                ActionListItem(
                    headline = Loc.worktime_setSchedule,
                    supportingText = "Manage weekly shift times",
                    leadingContent = { Icon(Icons.Default.DateRange, null) },
                    onClick = { onNavigate(AppScreenEnum.WORK_TIME_SCHEDULE) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
                            text = "Reset All Work Settings?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "All your current work-time preferences, goals, and schedule configurations will be deleted to start fresh. Any existing shifts already saved in your calendar will remain until you manually reset them or they are overridden by a new schedule.",
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
                            Text("Reset Settings")
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = { showDeletePrompt = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Loc.general_cancel)
                        }
                    }
                }
            }

            ActionListItem(
                headline = "Reset Settings",
                supportingText = "Clear all work-time configurations",
                leadingContent = { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { showDeletePrompt = true }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
