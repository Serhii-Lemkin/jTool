package com.jtool.android.features.calendar.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.calendar.settings.components.sync_info_sheet.SyncInfoSheet
import com.jtool.android.features.calendar.settings.components.calendar_picker_sheet.CalendarPickerSheet
import com.jtool.android.features.calendar.settings.components.calendar_permission_sheet.CalendarPermissionSheet
import com.jtool.android.features.calendar.settings.components.manual_sync_sheet.ManualSyncSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.features.calendar.settings.components.sync_loader_sheet.SyncLoaderSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarSettingsScreen(
    dataManager: DataManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsManager(context) }
    
    var calendarConfig by remember { mutableStateOf(settings.getCalendarConfig()) }
    
    var showSyncInfo by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var showManualSyncSheet by remember { mutableStateOf(false) }
    
    var availableCalendars by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }
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

    LaunchedEffect(calendarConfig.googleSyncEnabled) {
        if (calendarConfig.googleSyncEnabled && context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            availableCalendars = GoogleCalendarSyncHelper.getAvailableCalendars(context)
        }
    }

    if (showSyncInfo) {
        SyncInfoSheet(onDismiss = { showSyncInfo = false })
    }

    if (isSyncing) {
        SyncLoaderSheet(onDismiss = { isSyncing = false })
    }

    if (showCalendarPicker) {
        CalendarPickerSheet(
            availableCalendars = availableCalendars,
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

    if (showManualSyncSheet) {
        ManualSyncSheet(
            pullEnabled = calendarConfig.googlePullEnabled,
            pushEnabled = calendarConfig.googlePushEnabled,
            isSyncing = isSyncing,
            syncStatus = syncStatus,
            onConfirm = { from, to ->
                syncStatus = null
                performFullSync(context, dataManager, settings, scope, from, to, { isSyncing = it }, { syncStatus = it })
            },
            onDismiss = { 
                showManualSyncSheet = false 
                syncStatus = null
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = Loc.Calendar.syncTitle, 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ActionListItem(
                headline = Loc.Calendar.syncInfo,
                supportingText = Loc.Calendar.syncInfoSubtitle,
                leadingContent = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { showSyncInfo = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            ActionListItem(
                headline = Loc.Calendar.enableSync,
                supportingText = Loc.Calendar.enableSyncSubtitle,
                trailingContent = {
                    Switch(
                        checked = calendarConfig.googleSyncEnabled,
                        onCheckedChange = { 
                            val newConfig = calendarConfig.copy(googleSyncEnabled = it)
                            calendarConfig = newConfig
                            settings.saveCalendarConfig(newConfig)
                            scope.launch { dataManager.pushUserSettings() }
                            
                            if (it && context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                                showPermissionSheet = true
                            }
                        }
                    )
                },
                onClick = {
                    val newEnabled = !calendarConfig.googleSyncEnabled
                    val newConfig = calendarConfig.copy(googleSyncEnabled = newEnabled)
                    calendarConfig = newConfig
                    settings.saveCalendarConfig(newConfig)
                    scope.launch { dataManager.pushUserSettings() }
                    
                    if (newEnabled && context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                        showPermissionSheet = true
                    }
                }
            )

            if (calendarConfig.googleSyncEnabled) {
                val selectedCalendarName = availableCalendars.find { it.first == calendarConfig.googleCalendarId }?.second ?: Loc.Calendar.noneSelected
                ActionListItem(
                    headline = Loc.Calendar.selectCalendar,
                    supportingText = selectedCalendarName,
                    leadingContent = { Icon(Icons.Default.CalendarToday, null) },
                    onClick = { 
                        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                            availableCalendars = GoogleCalendarSyncHelper.getAvailableCalendars(context)
                            showCalendarPicker = true
                        } else {
                            showPermissionSheet = true
                        }
                    }
                )

                ActionListItem(
                    headline = Loc.Calendar.pullEvents,
                    supportingText = Loc.Calendar.pullEventsSubtitle,
                    trailingContent = {
                        Checkbox(
                            checked = calendarConfig.googlePullEnabled,
                            onCheckedChange = { 
                                val newConfig = calendarConfig.copy(googlePullEnabled = it)
                                calendarConfig = newConfig
                                settings.saveCalendarConfig(newConfig)
                                scope.launch { dataManager.pushUserSettings() }
                            }
                        )
                    },
                    onClick = {
                        val newConfig = calendarConfig.copy(googlePullEnabled = !calendarConfig.googlePullEnabled)
                        calendarConfig = newConfig
                        settings.saveCalendarConfig(newConfig)
                        scope.launch { dataManager.pushUserSettings() }
                    }
                )

                ActionListItem(
                    headline = Loc.Calendar.pushEvents,
                    supportingText = Loc.Calendar.pushEventsSubtitle,
                    trailingContent = {
                        Checkbox(
                            checked = calendarConfig.googlePushEnabled,
                            onCheckedChange = { 
                                val newConfig = calendarConfig.copy(googlePushEnabled = it)
                                calendarConfig = newConfig
                                settings.saveCalendarConfig(newConfig)
                                scope.launch { dataManager.pushUserSettings() }
                            }
                        )
                    },
                    onClick = {
                        val newConfig = calendarConfig.copy(googlePushEnabled = !calendarConfig.googlePushEnabled)
                        calendarConfig = newConfig
                        settings.saveCalendarConfig(newConfig)
                        scope.launch { dataManager.pushUserSettings() }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = Loc.Calendar.smartWorkDetection, 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = Loc.Calendar.keywordsExpl, 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = calendarConfig.autoWorkKeywords,
                onValueChange = { 
                    val singleWord = it.trim().split(" ").firstOrNull() ?: ""
                    val newConfig = calendarConfig.copy(autoWorkKeywords = singleWord)
                    calendarConfig = newConfig
                    settings.saveCalendarConfig(newConfig)
                    scope.launch { dataManager.pushUserSettings() }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(Loc.Calendar.keywordsLabel) },
                placeholder = { Text("") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                        showManualSyncSheet = true
                    } else {
                        showPermissionSheet = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(Loc.Calendar.manualSyncAction)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun performFullSync(
    context: Context,
    dataManager: DataManager,
    settings: SettingsManager,
    scope: CoroutineScope,
    fromDate: LocalDate,
    toDate: LocalDate,
    setLoading: (Boolean) -> Unit,
    setStatus: (String?) -> Unit
) {
    val calendarId = settings.getGoogleCalendarId()
    if (calendarId.isEmpty() || calendarId == "primary") {
        setStatus(Loc.Calendar.selectCalendarSubtitle)
        return
    }

    setLoading(true)
    setStatus(Loc.Calendar.syncing)
    scope.launch(Dispatchers.IO) {
        try {
            val userId = settings.getUserId()
            var pulledCount = 0
            var pushedCount = 0
            
            // Pull Logic
            val startTime = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTime = toDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (settings.isGoogleCalendarPullEnabled()) {
                val remoteEvents = GoogleCalendarSyncHelper.pullFromGoogleCalendar(context, userId, calendarId, startTime, endTime)
                
                if (remoteEvents != null) {
                    val localEvents = dataManager.allCalendarEventsOneTime(startTime, endTime)
                    val remoteGoogleIds = remoteEvents.map { it.googleEventId }.toSet()

                    // 1. Sync Pull
                    remoteEvents.forEach { remote ->
                        val existing = localEvents.find { it.googleEventId == remote.googleEventId } 
                            ?: localEvents.find { local ->
                                local.googleEventId == null &&
                                local.title.equals(remote.title, ignoreCase = true) &&
                                local.dtStart == remote.dtStart
                            }

                        if (existing == null) {
                            dataManager.saveCalendarEvent(remote)
                            pulledCount++
                        } else if (existing.googleEventId == null) {
                            dataManager.saveCalendarEvent(existing.copy(googleEventId = remote.googleEventId, googleCalendarId = calendarId))
                        }
                    }

                    // 2. Cleanup missing from Google (only if they had googleEventId)
                    localEvents.forEach { local ->
                        if (local.googleEventId != null && !remoteGoogleIds.contains(local.googleEventId)) {
                            dataManager.deleteCalendarEvent(local.id)
                        }
                    }
                }
            }
            
            // Push Logic
            if (settings.isGoogleCalendarPushEnabled()) {
                val keywords = settings.getAutoWorkKeywords().split(",").map { it.trim().lowercase() }
                val localEvents = dataManager.allCalendarEventsOneTime(startTime, endTime)
                
                localEvents.forEach { event ->
                    if (event.googleEventId == null && event.dtStart != null) {
                        var eventToPush = event
                        val matchedKeyword = keywords.find { it.isNotEmpty() && eventToPush.title.lowercase().contains(it) }
                        if (matchedKeyword != null && !eventToPush.note.contains(matchedKeyword, ignoreCase = true)) {
                            val newNote = if (eventToPush.note.isEmpty()) matchedKeyword else "${eventToPush.note}\n$matchedKeyword"
                            eventToPush = eventToPush.copy(note = newNote)
                        }

                        val gId = GoogleCalendarSyncHelper.pushToGoogleCalendar(context, eventToPush, calendarId)
                        if (gId != null) {
                            val updated = eventToPush.copy(googleEventId = gId.toString(), googleCalendarId = calendarId)
                            dataManager.saveCalendarEvent(updated)
                            pushedCount++
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                setLoading(false)
                val summary = "Pulled: $pulledCount, Pushed: $pushedCount"
                setStatus("${Loc.Calendar.syncCompleted}\n$summary")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                setLoading(false)
                setStatus(Loc.Calendar.syncFailed.format(e.message ?: Loc.General.unknown))
            }
        }
    }
}
