package com.jtool.android.features.calendar.grid

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.grid.components.CalendarGrid
import com.jtool.android.features.calendar.grid.components.CalendarGridActionSheet
import com.jtool.android.features.calendar.grid.components.DaysOfWeekHeader
import com.jtool.android.features.calendar.grid.components.EventItem
import com.jtool.android.features.calendar.grid.components.MonthHeader
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGridScreen(
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarGridViewModel,
    groups: Map<String, GroupInfo>
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val workShiftColorKey = settingsManager.getWorkShiftColorKey()
    var showActionSheet by remember { mutableStateOf(false) }
    var showTypeSelectionSheet by remember { mutableStateOf(false) }
    var isFullScreenGrid by remember { mutableStateOf(settingsManager.isCalendarGridOpened()) }

    val setGridOpened: (Boolean) -> Unit = { opened ->
        if (isFullScreenGrid != opened) {
            isFullScreenGrid = opened
            settingsManager.setCalendarGridOpened(opened)
        }
    }

    val gridGestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                if (dragAmount > 20f) {
                    setGridOpened(true)
                } else if (dragAmount < -20f) {
                    setGridOpened(false)
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (uiState.showAddEditDialog) {
        SideEffect {
            onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
            viewModel.toggleAddEditDialog(false)
        }
    }

    if (showActionSheet) {
        CalendarGridActionSheet(
            showDeclinedEvents = uiState.showDeclinedEvents,
            onToggleShowDeclined = { viewModel.toggleShowDeclinedEvents() },
            dataManager = viewModel.dataManager,
            onRefreshData = { viewModel.refresh() },
            onLearnMore = { com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, null, "calendar") },
            onDismiss = { showActionSheet = false }
        )
    }

    if (showTypeSelectionSheet) {
        com.jtool.android.features.calendar.components.event_type_selection_sheet.EventTypeSelectionSheet(
            onSelectType = { type ->
                when (type) {
                    com.jtool.android.features.calendar.components.event_type_selection_sheet.CalendarCreationType.WORK_SHIFT -> {
                        val shiftTitle = settingsManager.getCustomWorkShiftTitle()
                        val shiftColor = settingsManager.getWorkShiftColorKey()
                        val options = settingsManager.getWorktimeOptions()
                        val defaultStart = options.scheduleStart ?: "09:00"
                        val defaultEnd = options.scheduleEnd ?: "17:00"
                        val nowLdt = java.time.LocalDateTime.of(uiState.selectedDate, java.time.LocalTime.parse(defaultStart))
                        val startMs = nowLdt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val endMs = java.time.LocalDateTime.of(uiState.selectedDate, java.time.LocalTime.parse(defaultEnd)).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                        val preset = CalendarEvent(
                            title = shiftTitle,
                            colorKey = shiftColor,
                            dtStart = startMs,
                            dtEnd = endMs
                        )
                        viewModel.toggleAddEditDialog(true, preset)
                    }
                    com.jtool.android.features.calendar.components.event_type_selection_sheet.CalendarCreationType.REMINDER -> {
                        val preset = CalendarEvent(
                            reminderMinutes = 15
                        )
                        viewModel.toggleAddEditDialog(true, preset)
                    }
                    com.jtool.android.features.calendar.components.event_type_selection_sheet.CalendarCreationType.EVENT -> {
                        viewModel.toggleAddEditDialog(true, null)
                    }
                }
                showTypeSelectionSheet = false
            },
            onDismiss = { showTypeSelectionSheet = false }
        )
    }

    BaseScreen(
        title = Loc.Calendar.calendar,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onRefresh = { viewModel.refresh() },
        actions = {
            TitleActionIcon(
                icon = Icons.Default.Refresh,
                onClick = { viewModel.refresh() }
            )
            TitleActionIcon(
                icon = Icons.Default.MoreVert,
                onClick = { showActionSheet = true }
            )
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { showTypeSelectionSheet = true },
                contentDescription = Loc.Calendar.addEvent
            )
        }
    ) { _ ->
        val hasSelectedCalendars = remember(uiState, showActionSheet) {
            viewModel.settingsManager.getDisplayedCalendarIds().isNotEmpty() ||
                viewModel.settingsManager.getGoogleCalendarId().isNotEmpty()
        }
        if (!hasSelectedCalendars) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(Loc.Calendar.selectCalendarSubtitle, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showActionSheet = true }) {
                        Text(Loc.Calendar.selectCalendar)
                    }
                }
            }
            return@BaseScreen
        }

        val initialMonth = remember { uiState.selectedMonth }
        val pagerState = rememberPagerState(
            initialPage = 1000,
            pageCount = { 2000 }
        )
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { settledPage ->
                val monthOffset = settledPage - 1000
                val targetMonth = initialMonth.plusMonths(monthOffset.toLong())
                if (targetMonth != uiState.selectedMonth) {
                    viewModel.onMonthChange(targetMonth)
                }
            }
        }

        LaunchedEffect(uiState.selectedMonth) {
            val expectedPage = 1000 + (uiState.selectedMonth.year * 12 + uiState.selectedMonth.monthValue) - (initialMonth.year * 12 + initialMonth.monthValue)
            if (pagerState.currentPage != expectedPage) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(expectedPage)
                }
            }
        }

        AnimatedContent(
            targetState = isFullScreenGrid,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150))
            },
            modifier = Modifier
                .fillMaxSize()
                .then(gridGestureModifier),
            label = "calendarGridStateTransition"
        ) { fullScreen ->
            if (fullScreen) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    MonthHeader(
                        currentMonth = uiState.selectedMonth,
                        onPrev = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        onNext = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { page ->
                        val monthOffset = page - 1000
                        val pageMonth = initialMonth.plusMonths(monthOffset.toLong())
                        Column(modifier = Modifier.fillMaxSize()) {
                            DaysOfWeekHeader()
                            Spacer(Modifier.height(8.dp))
                            CalendarGrid(
                                selectedMonth = pageMonth,
                                selectedDate = uiState.selectedDate,
                                eventSummaries = uiState.eventSummaries,
                                isFullScreenGrid = true,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                onDateSelected = { date ->
                                    viewModel.onDateSelected(date)
                                    onNavigate(AppScreenEnum.CALENDAR_DAY)
                                }
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MonthHeader(
                            currentMonth = uiState.selectedMonth,
                            onPrev = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            onNext = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            val monthOffset = page - 1000
                            val pageMonth = initialMonth.plusMonths(monthOffset.toLong())
                            Column(modifier = Modifier.fillMaxWidth()) {
                                DaysOfWeekHeader()
                                Spacer(Modifier.height(8.dp))
                                CalendarGrid(
                                    selectedMonth = pageMonth,
                                    selectedDate = uiState.selectedDate,
                                    eventSummaries = uiState.eventSummaries,
                                    isFullScreenGrid = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    onDateSelected = { date ->
                                        viewModel.onDateSelected(date)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val selectedDateEvents = uiState.privateEvents.filter { it.date == uiState.selectedDate.toString() }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val formattedSelectedDate = remember(uiState.selectedDate) {
                                uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = Loc.Calendar.eventsFor.format(formattedSelectedDate),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                TextButton(onClick = { onNavigate(AppScreenEnum.CALENDAR_DAY) }) {
                                    Icon(Icons.Default.CalendarViewDay, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(Loc.Calendar.goToDayView, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (selectedDateEvents.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(Loc.Calendar.noEvents, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(selectedDateEvents, key = { it.id }) { event ->
                                val extension = CalendarEventHelper.findExtensionForEvent(event, uiState.extensions)
                                EventItem(
                                    event = event,
                                    extension = extension,
                                    groups = groups,
                                    workShiftColorKey = workShiftColorKey,
                                    onClick = {
                                        viewModel.onEventClicked(event)
                                        onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                                    }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}