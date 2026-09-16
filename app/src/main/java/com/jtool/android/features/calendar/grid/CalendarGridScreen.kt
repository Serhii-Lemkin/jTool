package com.jtool.android.features.calendar.grid

import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.jtool.android.core.components.layout.GenericTitle
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

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (uiState.showAddEditDialog) {
        SideEffect {
            onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
            viewModel.toggleAddEditDialog(false, uiState.editingEvent)
        }
    }

    if (showActionSheet) {
        CalendarGridActionSheet(
            showDeclinedEvents = uiState.showDeclinedEvents,
            onToggleShowDeclined = { viewModel.toggleShowDeclinedEvents() },
            onSettings = { onNavigate(AppScreenEnum.CALENDAR_SETTINGS) },
            onDismiss = { showActionSheet = false }
        )
    }

    Scaffold(
        topBar = {
            GenericTitle(
                title = Loc.Calendar.calendar,
                onBack = onBack,
                actions = {
                    TitleActionIcon(
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.refresh() }
                    )
                    TitleActionIcon(
                        icon = Icons.Default.MoreVert,
                        onClick = { showActionSheet = true }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddEditDialog(true) }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        val calendarId = viewModel.settingsManager.getGoogleCalendarId()
        if (calendarId.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(Loc.Calendar.selectCalendarSubtitle, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { onNavigate(AppScreenEnum.CALENDAR_SETTINGS) }) {
                        Text(Loc.Calendar.selectCalendar)
                    }
                }
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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

                    Spacer(Modifier.height(16.dp))

                    val gridStyle = remember(uiState) { settingsManager.getGridEventStyle() }
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
                                gridStyle = gridStyle,
                                onDateSelected = {
                                    viewModel.onDateSelected(it)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val selectedDateEvents = uiState.privateEvents.filter { it.date == uiState.selectedDate.toString() }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
        val days = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
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
    gridStyle: GridEventStyleEnum,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = selectedMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = selectedMonth.lengthOfMonth()

    val totalCells = daysInMonth + firstDayOfWeek
    val rows = (totalCells + 6) / 7

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(if (gridStyle == GridEventStyleEnum.CHIPS) 2.dp else 4.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayOfMonth = index - firstDayOfWeek + 1
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = selectedMonth.atDay(dayOfMonth)
                            DayCell(
                                date = date,
                                summary = eventSummaries[date.toString()],
                                isSelected = date == selectedDate,
                                gridStyle = gridStyle,
                                onClick = { onDateSelected(date) }
                            )
                        } else {
                            Box(modifier = Modifier.height(if (gridStyle == GridEventStyleEnum.CHIPS) 76.dp else 40.dp))
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
    gridStyle: GridEventStyleEnum,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val cellModifier = if (gridStyle == GridEventStyleEnum.CHIPS) {
        Modifier
            .fillMaxWidth()
            .height(76.dp)
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
                if (gridStyle == GridEventStyleEnum.DOTS) {
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
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.dp, start = 1.dp, end = 1.dp)
                    ) {
                        summary.eventPreviews.take(2).forEach { preview ->
                            val eventColor = getEventColor(preview.colorKey)
                            Surface(
                                color = eventColor,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(13.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
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
                                        modifier = Modifier.weight(1f),
                                        style = ComposeTextStyle(
                                            platformStyle = PlatformTextStyle(
                                                includeFontPadding = false
                                            )
                                        )
                                    )
                                    if (!preview.groupTagName.isNullOrEmpty()) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(2.dp)
                                        ) {
                                            Text(
                                                text = preview.groupTagName,
                                                fontSize = 6.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (summary.eventCount > 2) {
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