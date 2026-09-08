package com.jtool.android.features.calendar.day

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.components.AddEditEventDialog
import com.jtool.android.features.calendar.components.TimelineEventCard
import com.jtool.android.features.calendar.components.TimelineHourMarkers
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDayScreen(
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarDayViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
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

    LaunchedEffect(uiState.selectedDate) {
        viewModel.refresh()
        val startHour = if (uiState.selectedDate == LocalDate.now()) {
            (LocalTime.now().hour - 1).coerceAtLeast(0)
        } else {
            viewModel.getWorkdayStartHour()
        }
        val scrollPos = with(density) { (startHour * hourHeight).dp.toPx() }
        scrollState.scrollTo(scrollPos.toInt())
    }

    if (uiState.showAddEditDialog) {
        SideEffect {
            onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
            viewModel.toggleAddEditDialog(false, uiState.editingEvent)
        }
    }

    Scaffold(
        topBar = {
            val dateTitle = remember(uiState.selectedDate, Loc.lang) {
                val day = uiState.selectedDate.dayOfMonth
                val month = Loc.getMonthName(uiState.selectedDate.month, TextStyle.SHORT)
                val year = uiState.selectedDate.year
                "$day $month $year"
            }
            GenericTitle(
                title = dateTitle,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.viewMode == CalendarDayViewMode.TIMELINE) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewTimeline,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { viewModel.navigateDay(-1) }) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    IconButton(onClick = { viewModel.navigateDay(1) }) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            )
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { viewModel.toggleAddEditDialog(true) },
                contentDescription = Loc.Calendar.addEvent
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.viewMode == CalendarDayViewMode.TIMELINE) {
                TimelineView(uiState, currentTime, hourHeight, scrollState, onNavigate, viewModel)
            } else {
                ListView(uiState, onNavigate, viewModel)
            }
        }
    }
}

@Composable
private fun TimelineView(
    uiState: CalendarDayUiState,
    currentTime: LocalTime,
    hourHeight: Int,
    scrollState: ScrollState,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarDayViewModel
) {
    val allDayEvents = uiState.privateEvents.filter { it.allDay }
    val timedEvents = uiState.privateEvents.filter { !it.allDay }
    
    val overlapGroups = remember(timedEvents) {
        CalendarEventHelper.calculateOverlapGroups(timedEvents)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (allDayEvents.isNotEmpty()) {
            AllDaySection(allDayEvents, onNavigate, viewModel)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            TimelineHourMarkers(hourHeight)

            if (uiState.selectedDate == LocalDate.now()) {
                val timeTop = (currentTime.hour * hourHeight) + (currentTime.minute * hourHeight / 60)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = timeTop.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.error, shape = CircleShape))
                    HorizontalDivider(color = MaterialTheme.colorScheme.error, thickness = 2.dp, modifier = Modifier.fillMaxWidth())
                }
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp)
            ) {
                val containerWidth = maxWidth
                timedEvents.forEach { event ->
                    val groupInfo = overlapGroups[event.id] ?: Pair(0, 1)
                    val column = groupInfo.first
                    val totalCols = groupInfo.second
                    
                    val eventTop = CalendarEventHelper.getEventTop(event.startTime, hourHeight)
                    val eventHeight = CalendarEventHelper.getEventHeight(event.startTime, event.endTime, hourHeight)
                    val cardWidth = containerWidth / totalCols

                    Box(
                        modifier = Modifier
                            .padding(top = eventTop.dp)
                            .offset(x = (column * (containerWidth.value / totalCols)).dp)
                            .width(cardWidth)
                            .height(eventHeight.dp)
                            .padding(horizontal = 1.dp, vertical = 0.5.dp)
                    ) {
                        TimelineEventCard(
                            event = event,
                            onClick = {
                                viewModel.toggleAddEditDialog(false, event)
                                if (event.id.startsWith("virtual_")) onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
                                else onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllDaySection(
    events: List<CalendarEvent>,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarDayViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = Loc.Calendar.allDay, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        events.forEach { event ->
            Surface(
                onClick = { 
                    viewModel.toggleAddEditDialog(false, event)
                    onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                },
                color = if (event.type == CalendarEventTypeEnum.SHIFT) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (event.type == CalendarEventTypeEnum.SHIFT) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ListView(
    uiState: CalendarDayUiState,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarDayViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.privateEvents.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    Text(Loc.Calendar.noEvents, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(uiState.privateEvents.sortedBy { if (it.allDay) 0 else 1 }.sortedBy { it.startTime }, key = { it.id }) { event ->
                Card(
                    onClick = {
                        viewModel.toggleAddEditDialog(false, event)
                        if (event.id.startsWith("virtual_")) onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
                        else onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (event.allDay) "ALL" else event.startTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            if (event.allDay) {
                                Text(Loc.Calendar.allDay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            } else if (!event.endTime.isNullOrEmpty()) {
                                Text(event.endTime!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
