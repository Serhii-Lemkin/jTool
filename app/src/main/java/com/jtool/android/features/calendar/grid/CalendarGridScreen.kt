package com.jtool.android.features.calendar.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.DayEventSummary
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.settings.SettingsManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGridScreen(
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarGridViewModel,
    settingsManager: SettingsManager
) {
    val uiState by viewModel.uiState.collectAsState()
    val groups = remember { settingsManager.getGroups() }
    var showFilterSheet by remember { mutableStateOf(false) }

    if (uiState.showAddEditDialog) {
        SideEffect {
            onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
            viewModel.toggleAddEditDialog(false, uiState.editingEvent)
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = Loc.Calendar.selectGroups,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                groups.values.filter { !it.isPrivate }.forEach { group ->
                    val isVisible = uiState.visibleGroupIds.isEmpty() || uiState.visibleGroupIds.contains(group.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleGroupVisibility(group.id) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { viewModel.toggleGroupVisibility(group.id) }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            GenericTitle(
                title = Loc.Calendar.calendar,
                onBack = onBack,
                actions = {
                    TitleActionIcon(
                        icon = Icons.Default.FilterList,
                        onClick = { showFilterSheet = true }
                    )
                    TitleActionIcon(
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.refresh() }
                    )
                    TitleActionIcon(
                        icon = Icons.Default.Settings,
                        onClick = { onNavigate(AppScreenEnum.CALENDAR_SETTINGS) }
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
        if (uiState.syncResultMessage != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.clearSyncMessage() },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isSuccess = uiState.syncResultMessage == Loc.Calendar.syncCompleted
                    Icon(
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        tint = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = uiState.syncResultMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { viewModel.clearSyncMessage() }, modifier = Modifier.fillMaxWidth()) {
                        Text(Loc.General.close)
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MonthHeader(
                        currentMonth = uiState.selectedMonth,
                        onMonthChange = { viewModel.onMonthChange(it) }
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    DaysOfWeekHeader()
                    
                    CalendarGrid(
                        selectedMonth = uiState.selectedMonth,
                        selectedDate = uiState.selectedDate,
                        eventSummaries = uiState.eventSummaries,
                        onDateSelected = { 
                            viewModel.onDateSelected(it)
                        }
                    )
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
                            EventItem(
                                event = event,
                                settingsManager = settingsManager,
                                onClick = { 
                                    viewModel.onEventClicked(event)
                                    if (event.id.startsWith("virtual_")) {
                                        onNavigate(AppScreenEnum.CALENDAR_ADD_EDIT)
                                    } else {
                                        onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                                    }
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
private fun EventItem(event: CalendarEvent, settingsManager: SettingsManager, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (event.type == CalendarEventTypeEnum.SHIFT) Icons.Default.Work else Icons.Default.Event,
                contentDescription = null,
                tint = if (event.type == CalendarEventTypeEnum.SHIFT) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (event.allDay) {
                    Text(Loc.Calendar.allDay ?: "All Day", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("${event.startTime} - ${event.endTime ?: ""}", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            if (event.sharedGroupIds.isNotEmpty()) {
                val groups = settingsManager.getGroups()
                Column(horizontalAlignment = Alignment.End) {
                    event.sharedGroupIds.forEach { gid ->
                        val group = groups[gid]
                        if (group != null) {
                            AssistChip(
                                onClick = {},
                                label = { Text(if (group.isPrivate) "Private" else group.name, fontSize = 10.sp) },
                                modifier = Modifier.height(20.dp).padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, null)
        }
        
        Text(
            text = "${Loc.getMonthName(currentMonth.month)} ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
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
                text = Loc.getDayOfWeekName(day, TextStyle.SHORT),
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
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = selectedMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = selectedMonth.lengthOfMonth()
    
    val totalCells = daysInMonth + firstDayOfWeek
    val rows = (totalCells + 6) / 7
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth().height(280.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        userScrollEnabled = false
    ) {
        items(rows * 7) { index ->
            val dayOfMonth = index - firstDayOfWeek + 1
            if (dayOfMonth in 1..daysInMonth) {
                val date = selectedMonth.atDay(dayOfMonth)
                DayCell(
                    date = date,
                    summary = eventSummaries[date.toString()],
                    isSelected = date == selectedDate,
                    onClick = { onDateSelected(date) }
                )
            } else {
                Box(modifier = Modifier.aspectRatio(1f))
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, summary: DayEventSummary?, isSelected: Boolean, onClick: () -> Unit) {
    val isToday = date == LocalDate.now()
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                else if (isToday) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(2.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            
            if (summary != null) {
                val shifts = summary.typeCounts[CalendarEventTypeEnum.SHIFT] ?: 0
                val common = summary.typeCounts[CalendarEventTypeEnum.COMMON] ?: 0
                val total = shifts + common

                if (total > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val shiftDots = shifts.coerceAtMost(2)
                        val commonDots = common.coerceAtMost(4 - shiftDots)
                        
                        repeat(shiftDots) {
                            Box(Modifier.size(4.dp).background(Color(0xFF4CAF50), CircleShape))
                        }
                        repeat(commonDots) {
                            Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}
