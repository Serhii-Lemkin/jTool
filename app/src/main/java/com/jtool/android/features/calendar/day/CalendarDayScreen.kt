package com.jtool.android.features.calendar.day

import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.AppLoader
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.components.TimelineEventCard
import com.jtool.android.features.calendar.components.TimelineHourMarkers
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getEventColor
import com.jtool.android.features.calendar.grid.components.CalendarGridActionSheet
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDayScreen(
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarDayViewModel,
    groups: Map<String, GroupInfo>
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val currentWorkShiftColorKey = remember { settingsManager.getWorkShiftColorKey() }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val hourHeight = 90 // dp

    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60_000)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(uiState.selectedDate) {
        viewModel.refresh()
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            val startHour = viewModel.getWorkdayStartHour()
            scrollState.scrollTo((startHour * hourHeight * density.density).toInt())
        }
    }

    if (uiState.showAddEditDialog) {
        SideEffect {
            onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
            viewModel.toggleAddEditDialog(false, uiState.editingEvent)
        }
    }

    var showDayActionSheet by remember { mutableStateOf(false) }

    if (showDayActionSheet) {
        CalendarGridActionSheet(
            showDeclinedEvents = uiState.showDeclinedEvents,
            onToggleShowDeclined = { viewModel.toggleShowDeclinedEvents() },
            onSettings = { onNavigate(AppScreenEnum.CALENDAR_SETTINGS) },
            onDismiss = { showDayActionSheet = false }
        )
    }

    BaseScreen(
        title = uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM", Locale(Loc.lang))),
        isLoading = uiState.isLoading,
        onBack = onBack,
        onRefresh = { viewModel.refresh() },
        actions = {
            IconButton(onClick = { viewModel.toggleViewMode() }) {
                Icon(
                    imageVector = if (uiState.viewMode == CalendarDayViewMode.TIMELINE) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewTimeline,
                    contentDescription = null
                )
            }
            IconButton(onClick = { showDayActionSheet = true }) {
                Icon(Icons.Default.MoreVert, null)
            }
            IconButton(onClick = { viewModel.navigateDay(-1) }) {
                Icon(Icons.Default.ChevronLeft, null)
            }
            IconButton(onClick = { viewModel.navigateDay(1) }) {
                Icon(Icons.Default.ChevronRight, null)
            }
        },
        floatingActionButton = {
            UnifiedFAB(
                icon = Icons.Default.Add,
                onClick = { viewModel.toggleAddEditDialog(true) }
            )
        }
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.viewMode == CalendarDayViewMode.TIMELINE) {
                TimelineView(
                    uiState = uiState,
                    groups = groups,
                    scrollState = scrollState,
                    hourHeight = hourHeight,
                    currentTime = currentTime,
                    onEventClick = {
                        viewModel.toggleAddEditDialog(false, it)
                        onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                    }
                )
            } else {
                ListView(
                    uiState = uiState,
                    groups = groups,
                    viewModel = viewModel,
                    workShiftColorKey = currentWorkShiftColorKey,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun TimelineView(
    uiState: CalendarDayUiState,
    groups: Map<String, GroupInfo>,
    scrollState: ScrollState,
    hourHeight: Int,
    currentTime: LocalTime,
    onEventClick: (CalendarEvent) -> Unit
) {
    val allDayEvents = uiState.privateEvents.filter { it.allDay }
    val timedEvents = uiState.privateEvents.filter { !it.allDay }

    Column(modifier = Modifier.fillMaxSize()) {
        if (allDayEvents.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 130.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allDayEvents.forEach { event ->
                    TimelineEventCard(
                        event = event,
                        groups = groups,
                        onClick = { onEventClick(event) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    )
                }
            }
            HorizontalDivider()
        }

        Box(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
            TimelineHourMarkers(hourHeight = hourHeight)

            val overlapGroups = remember(timedEvents) { CalendarEventHelper.calculateOverlapGroups(timedEvents) }

            timedEvents.forEach { event ->
                val groupInfo = overlapGroups[event.id] ?: Pair(0, 1)
                val widthPercent = 1f / groupInfo.second
                val leftOffset = groupInfo.first * widthPercent

                val eventTop = CalendarEventHelper.getEventTop(event.startTime, hourHeight)
                val eventHeight = CalendarEventHelper.getEventHeight(event.startTime, event.endTime, hourHeight)

                Box(
                    modifier = Modifier
                        .padding(start = 60.dp) // Offset for hour markers
                        .fillMaxWidth()
                        .height(eventHeight.dp)
                        .offset(y = eventTop.dp)
                        .padding(horizontal = 2.dp)
                ) {
                    TimelineEventCard(
                        event = event,
                        groups = groups,
                        onClick = { onEventClick(event) },
                        modifier = Modifier
                            .fillMaxWidth(widthPercent)
                            .offset(x = (leftOffset * 100).dp)
                    )
                }
            }

            // Current time indicator
            if (uiState.selectedDate == LocalDate.now()) {
                val nowTop = CalendarEventHelper.getEventTop(currentTime.toString(), hourHeight)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = nowTop.dp)
                        .background(MaterialTheme.colorScheme.error)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(x = 56.dp, y = (nowTop - 4).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

@Composable
private fun ListView(
    uiState: CalendarDayUiState,
    groups: Map<String, GroupInfo>,
    viewModel: CalendarDayViewModel,
    workShiftColorKey: String,
    onNavigate: (AppScreenEnum) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.privateEvents.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(Loc.Calendar.noEvents, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(uiState.privateEvents.sortedBy { if (it.allDay) 0 else 1 }.sortedBy { it.startTime }, key = { it.id }) { event ->
                val isDeclined = event.currentUserStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
                val extension = uiState.extensions[event.id]
                val themeColor = getEventColor(event.colorKey)
                val isWork = (event.colorKey != null && event.colorKey == workShiftColorKey) ||
                        (event.colorKey == null && workShiftColorKey == "1")

                Card(
                    onClick = {
                        viewModel.toggleAddEditDialog(false, event)
                        onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).alpha(if (isDeclined) 0.6f else 1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDeclined) MaterialTheme.colorScheme.error else themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isWork) Icons.Default.Work else Icons.Default.Event,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (isDeclined) TextDecoration.LineThrough else null
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (event.allDay) Loc.Calendar.allDay else "${event.startTime} - ${event.endTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val sharedGroupIds = extension?.sharedGroupIds ?: emptyList()
                        if (sharedGroupIds.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                val groupNames = sharedGroupIds.map { gid ->
                                    val group = groups[gid]
                                    if (group?.isPrivate == true) Loc.Calendar.private else group?.name ?: gid
                                }
                                val label = "${Loc.Calendar.belongsTo}: ${groupNames.joinToString(", ")}"

                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}