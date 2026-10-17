package com.jtool.android.features.calendar.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.calendar.settings.components.calendar_picker_sheet.CalendarPickerSheet
import com.jtool.android.features.calendar.settings.components.calendar_permission_sheet.CalendarPermissionSheet
import com.jtool.android.features.calendar.settings.components.reset_calendar_confirm_sheet.ResetCalendarConfirmSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.jtool.android.core.models.calendar.CalendarConfig
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.core.models.calendar.enums.GridEventStyleEnum
import com.jtool.android.features.calendar.settings.components.grid_style_picker_sheet.GridStylePickerSheet
import com.jtool.android.features.calendar.settings.components.multi_calendar_picker_sheet.MultiCalendarPickerSheet
import kotlinx.coroutines.launch

@Composable
fun CalendarSettingsScreen(
    dataManager: DataManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsManager(context) }
    
    var calendarConfig by remember { mutableStateOf(settings.getCalendarConfig()) }
    
    var availableCalendars by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var showMultiCalendarPicker by remember { mutableStateOf(false) }
    var showGridStylePicker by remember { mutableStateOf(false) }
    var showSharedCalendarPicker by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var showResetConfirmSheet by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        showPermissionSheet = false
        if (results.values.all { it }) {
            availableCalendars = GoogleCalendarSyncHelper.getAvailableCalendars(context)
            if (availableCalendars.isNotEmpty()) {
                showCalendarPicker = true
            }
        } else {
            infoMessage = Loc.Calendar.accessRequiredMsg to MessageType.ERROR
        }
    }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            availableCalendars = GoogleCalendarSyncHelper.getAvailableCalendars(context)
        }
    }

    if (showMultiCalendarPicker) {
        MultiCalendarPickerSheet(
            availableCalendars = availableCalendars.map { it.first to "${it.second} (${it.third})" },
            selectedCalendarIds = settings.getDisplayedCalendarIds(),
            onSaveCalendarIds = { ids ->
                settings.setDisplayedCalendarIds(ids)
                calendarConfig = settings.getCalendarConfig()
                scope.launch { dataManager.pushUserSettings() }
            },
            onDismiss = { showMultiCalendarPicker = false }
        )
    }

    if (showGridStylePicker) {
        GridStylePickerSheet(
            selectedStyle = calendarConfig.gridEventStyle,
            onSelectStyle = { style ->
                settings.setGridEventStyle(style)
                calendarConfig = settings.getCalendarConfig()
                scope.launch { dataManager.pushUserSettings() }
            },
            onDismiss = { showGridStylePicker = false }
        )
    }

    if (showCalendarPicker) {
        CalendarPickerSheet(
            availableCalendars = availableCalendars.map { it.first to "${it.second} (${it.third})" },
            selectedCalendarId = calendarConfig.googleCalendarId,
            onCalendarSelected = { id ->
                val newConfig = calendarConfig.copy(googleCalendarId = id)
                calendarConfig = newConfig
                settings.saveCalendarConfig(newConfig)
                scope.launch { dataManager.pushUserSettings() }
            },
            onDismiss = { showCalendarPicker = false }
        )
    }

    if (showSharedCalendarPicker) {
        CalendarPickerSheet(
            availableCalendars = availableCalendars.map { it.first to "${it.second} (${it.third})" },
            selectedCalendarId = calendarConfig.googleCalendarSharedId,
            onCalendarSelected = { id ->
                val account = availableCalendars.find { it.first == id }?.third ?: ""
                val newConfig = calendarConfig.copy(googleCalendarSharedId = id, googleCalendarEmail = account)
                calendarConfig = newConfig
                settings.saveCalendarConfig(newConfig)
                scope.launch { 
                    dataManager.updateUserCalendarEmailInGroups(account)
                    dataManager.pushUserSettings() 
                }
            },
            onDismiss = { showSharedCalendarPicker = false }
        )
    }

    if (showPermissionSheet) {
        CalendarPermissionSheet(
            onDismiss = { showPermissionSheet = false },
            onGrant = {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                ))
            }
        )
    }

    if (infoMessage != null) {
        MessageSheet(
            message = infoMessage!!.first,
            type = infoMessage!!.second,
            onDismiss = { infoMessage = null }
        )
    }

    BaseScreen(
        title = Loc.Launcher.settings,
        onBack = onBack
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = Loc.Calendar.calendar, 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val displayedCalendarIds = settings.getDisplayedCalendarIds()
            val displayedCalendarNames = remember(displayedCalendarIds, availableCalendars) {
                if (availableCalendars.isEmpty()) {
                    if (displayedCalendarIds.isNotEmpty()) "${displayedCalendarIds.size} calendars selected" else Loc.Calendar.noneSelected
                } else {
                    val names = availableCalendars.filter { displayedCalendarIds.contains(it.first) }.map { it.second }
                    if (names.isNotEmpty()) names.joinToString(", ") else Loc.Calendar.noneSelected
                }
            }
                
            ActionListItem(
                headline = CalendarLoc.displayedCalendars,
                supportingText = displayedCalendarNames,
                leadingContent = { Icon(Icons.Default.CalendarToday, null) },
                onClick = { 
                    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                        availableCalendars = GoogleCalendarSyncHelper.getAvailableCalendars(context)
                        showMultiCalendarPicker = true
                    } else {
                        showPermissionSheet = true
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            val selectedDestinationCalendarName = availableCalendars.find { it.first == calendarConfig.googleCalendarId }?.second 
                ?: if (calendarConfig.googleCalendarId.isNotEmpty()) calendarConfig.googleCalendarId else Loc.Calendar.noneSelected

            ActionListItem(
                headline = CalendarLoc.eventDestinationCalendar,
                supportingText = selectedDestinationCalendarName,
                leadingContent = { Icon(Icons.Default.EventAvailable, null) },
                onClick = { 
                    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                        availableCalendars = GoogleCalendarSyncHelper.getAvailableCalendars(context)
                        showCalendarPicker = true
                    } else {
                        showPermissionSheet = true
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            val currentGridStyle = remember(calendarConfig) { settings.getGridEventStyle() }
            val gridStyleLabel = if (currentGridStyle == GridEventStyleEnum.DOTS) CalendarLoc.gridEventStyleDots else CalendarLoc.gridEventStyleChips

            ActionListItem(
                headline = CalendarLoc.gridEventStyle,
                supportingText = gridStyleLabel,
                leadingContent = { Icon(Icons.Default.GridOn, null) },
                onClick = { showGridStylePicker = true },
                supportingMaxLines = Int.MAX_VALUE
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showResetConfirmSheet = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset Calendar Integration")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetConfirmSheet) {
        ResetCalendarConfirmSheet(
            onDismiss = { showResetConfirmSheet = false },
            onConfirm = {
                showResetConfirmSheet = false
                settings.saveCalendarConfig(CalendarConfig())
                calendarConfig = settings.getCalendarConfig()
                onBack()
            }
        )
    }
}
