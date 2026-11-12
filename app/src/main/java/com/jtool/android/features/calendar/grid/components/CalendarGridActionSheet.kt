package com.jtool.android.features.calendar.grid.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.core.localization.domains.GeneralLoc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import com.jtool.android.features.calendar.settings.components.calendar_picker_sheet.CalendarPickerSheet
import com.jtool.android.features.calendar.settings.components.multi_calendar_picker_sheet.MultiCalendarPickerSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGridActionSheet(
    showDeclinedEvents: Boolean = false,
    onToggleShowDeclined: () -> Unit = {},
    dataManager: DataManager? = null,
    onRefreshData: () -> Unit = {},
    onLearnMore: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsManager(context) }
    var calendarConfig by remember { mutableStateOf(settings.getCalendarConfig()) }

    var availableCalendars by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var showMultiCalendarPicker by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }

    val openCalendarsPicker: (Boolean) -> Unit = { isMulti ->
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            availableCalendars = GoogleCalendarSyncHelper.getAvailableCalendars(context)
            if (isMulti) showMultiCalendarPicker = true else showCalendarPicker = true
        }
    }

    if (showMultiCalendarPicker) {
        MultiCalendarPickerSheet(
            availableCalendars = availableCalendars.map { it.first to "${it.second} (${it.third})" },
            selectedCalendarIds = settings.getDisplayedCalendarIds(),
            onSaveCalendarIds = { ids ->
                settings.setDisplayedCalendarIds(ids)
                calendarConfig = settings.getCalendarConfig()
                scope.launch { dataManager?.pushUserSettings() }
                onRefreshData()
            },
            onDismiss = { showMultiCalendarPicker = false }
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
                scope.launch { dataManager?.pushUserSettings() }
                onRefreshData()
            },
            onDismiss = { showCalendarPicker = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionListItem(
                headline = CalendarLoc.displayedCalendars,
                supportingText = CalendarLoc.displayedCalendarsSubtitle,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    openCalendarsPicker(true)
                }
            )

            ActionListItem(
                headline = CalendarLoc.eventDestinationCalendar,
                supportingText = CalendarLoc.eventDestinationCalendarSubtitle,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    openCalendarsPicker(false)
                }
            )

            ActionListItem(
                headline = if (showDeclinedEvents) CalendarLoc.hideDeclinedEvents else Loc.s("calendar_showDeclinedEvents", "Show Declined Events"),
                supportingText = CalendarLoc.hideDeclinedEventsSubtitle,
                leadingContent = {
                    Icon(
                        imageVector = if (showDeclinedEvents) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    onToggleShowDeclined()
                    onDismiss()
                }
            )

            if (onLearnMore != null) {
                ActionListItem(
                    headline = GeneralLoc.learnMore,
                    supportingText = GeneralLoc.landingPageSubtitle,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        onLearnMore()
                        onDismiss()
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
