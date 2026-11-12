package com.jtool.android.features.calendar.grid

import android.provider.CalendarContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.calendar.DayEventSummary
import com.jtool.android.core.models.calendar.enums.GridEventStyleEnum
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getEventColor
import com.jtool.android.features.calendar.grid.components.CalendarGridActionSheet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as TimeTextStyle

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
        val calendarId = viewModel.settingsManager.getGoogleCalendarId()
        if (calendarId.isEmpty()) {
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

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
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
                                    val extension = uiState.extensions[event.id]
                                        ?: (if (event.id.startsWith("ext_")) uiState.extensions[event.id.removePrefix("ext_")] else null)
                                        ?: (if (event.iCalUID != null) uiState.extensions[event.iCalUID] else null)
                                        ?: uiState.extensions.values.find { it.googleEventId == event.id || (event.iCalUID != null && it.googleEventId == event.iCalUID) }
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
}

@Composable
private fun EventItem(
    event: CalendarEvent,
    extension: CalendarEventExtension?,
    groups: Map<String, GroupInfo>,
    workShiftColorKey: String,
    onClick: () -> Unit
) {
    val isDeclined = event.currentUserStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
    val themeColor = getEventColor(event.colorKey)
    val isWork = (event.colorKey != null && event.colorKey == workShiftColorKey) ||
            (event.colorKey == null && workShiftColorKey == "1")

    Card(
        onClick = onClick,
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
                if (event.allDay) {
                    Text(Loc.Calendar.allDay, style = MaterialTheme.typography.labelSmall)
                } else {
                    Text("${event.startTime} - ${event.endTime}", style = MaterialTheme.typography.bodySmall)
                }
            }

            val sharedGroupIds = extension?.sharedGroupIds ?: emptyList()
            if (sharedGroupIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    val groupNames = sharedGroupIds.map { gid ->
                        val group = groups[gid]
                        if (group?.isPrivate == true) Loc.Calendar.private else group?.name ?: gid
                    }
                    val label = "${Loc.Calendar.belongsTo}: ${groupNames.joinToString(", ")}"

                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, null)
        }

        Text(
            text = "${Loc.getMonthName(currentMonth.month)} ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val settingsManager = remember { SettingsManager(context) }
        val startDay = remember {
            try { DayOfWeek.valueOf(settingsManager.getWorktimeOptions().weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }
        }
        val allDays = DayOfWeek.entries
        val startIndex = allDays.indexOf(startDay)
        val days = allDays.subList(startIndex, allDays.size) + allDays.subList(0, startIndex)

        days.forEach { day ->
            Text(
                text = Loc.getDayOfWeekName(day, TimeTextStyle.SHORT),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    eventSummaries: Map<String, DayEventSummary>,
    isFullScreenGrid: Boolean,
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val startDay = remember {
        try { DayOfWeek.valueOf(settingsManager.getWorktimeOptions().weeklyStartDay) } catch (_: Exception) { DayOfWeek.MONDAY }
    }

    val firstDayOfMonth = selectedMonth.atDay(1)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value - startDay.value + 7) % 7
    val daysInMonth = selectedMonth.lengthOfMonth()

    val totalCells = daysInMonth + firstDayOfWeek
    val rows = (totalCells + 6) / 7

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = if (isFullScreenGrid) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayOfMonth = index - firstDayOfWeek + 1
                    Box(
                        modifier = if (isFullScreenGrid) Modifier.weight(1f).fillMaxHeight() else Modifier.weight(1f)
                    ) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = selectedMonth.atDay(dayOfMonth)
                            DayCell(
                                date = date,
                                summary = eventSummaries[date.toString()],
                                isSelected = date == selectedDate,
                                isFullScreenGrid = isFullScreenGrid,
                                onClick = { onDateSelected(date) }
                            )
                        } else {
                            Box(modifier = if (isFullScreenGrid) Modifier.fillMaxSize() else Modifier.aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    summary: DayEventSummary?,
    isSelected: Boolean,
    isFullScreenGrid: Boolean,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val cellModifier = if (isFullScreenGrid) {
        Modifier
            .fillMaxSize()
            .padding(1.dp)
            .clip(MaterialTheme.shapes.small)
    } else {
        Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(MaterialTheme.shapes.small)
    }

    Box(
        modifier = cellModifier
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else if (isToday) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize().padding(1.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )

            if (summary != null && summary.hasEvents) {
                if (!isFullScreenGrid) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val colorKeys = if (summary.eventColorKeys.isNotEmpty()) summary.eventColorKeys else listOf(null)
                        colorKeys.take(3).forEach { colorKey ->
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(getEventColor(colorKey), CircleShape)
                            )
                        }
                    }
                } else {
                    val maxShow = if (summary.eventCount <= 3) 3 else 2
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.dp, start = 1.dp, end = 1.dp)
                    ) {
                        summary.eventPreviews.take(maxShow).forEach { preview ->
                            val eventColor = getEventColor(preview.colorKey)
                            Surface(
                                color = eventColor,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(13.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 2.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = preview.title.ifEmpty { "Shared Event" },
                                        fontSize = 8.sp,
                                        lineHeight = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textDecoration = if (preview.isDeclined) TextDecoration.LineThrough else null,
                                        style = ComposeTextStyle(
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            )
                                        )
                                    )
                                }
                            }
                        }
                        if (summary.eventCount > maxShow) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(11.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "...",
                                        fontSize = 8.sp,
                                        lineHeight = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = ComposeTextStyle(
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}