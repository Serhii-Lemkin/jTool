package com.jtool.android.features.calendar.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.DayEventSummary
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.features.calendar.components.AddEditEventDialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGridScreen(
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: CalendarGridViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            GenericTitle(
                title = Loc.Calendar.calendar,
                onBack = onBack,
                actions = {
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            MonthSelector(uiState.selectedMonth, onMonthChange = viewModel::onMonthChange)
            
            val daysInMonth = uiState.selectedMonth.lengthOfMonth()
            val firstDayOfMonth = uiState.selectedMonth.atDay(1).dayOfWeek
            val offset = (firstDayOfMonth.value - 1) // 0 for Monday, 6 for Sunday
            
            val days = (1..daysInMonth).map { uiState.selectedMonth.atDay(it) }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.padding(horizontal = 8.dp).heightIn(max = 400.dp)
            ) {
                val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
                items(weekdays) { day ->
                    Text(day, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                items(offset) { Spacer(Modifier.size(48.dp)) }
                
                items(days) { date ->
                    DayCell(
                        date = date,
                        summary = uiState.eventSummaries[date.toString()],
                        isSelected = date == uiState.selectedDate,
                        onClick = { 
                            viewModel.onDateSelected(date)
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Day Summary Section
            val selectedDayEvents = uiState.privateEvents.filter { it.date == uiState.selectedDate.toString() }
                .sortedBy { it.startTime }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Loc.Calendar.calendar, // Or "Schedule"
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = uiState.selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { 
                        viewModel.onDateSelected(uiState.selectedDate)
                        onNavigate(AppScreenEnum.CALENDAR_DAY) 
                    }) {
                        Text(Loc.General.seeAll)
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (selectedDayEvents.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                        Text(Loc.Calendar.noEvents, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (event in selectedDayEvents.take(3)) {
                            val isVirtual = event.id.startsWith("virtual_")
                            ActionListItem(
                                headline = event.title.ifEmpty { "[${Loc.Calendar.calendar}]" },
                                supportingText = "${event.startTime}${event.endTime?.let { " - $it" } ?: ""}",
                                modifier = if (isVirtual) Modifier.alpha(0.6f) else Modifier,
                                leadingContent = {
                                    Icon(
                                        imageVector = if (event.type == CalendarEventTypeEnum.SHIFT) Icons.Default.Work else Icons.Default.Event,
                                        contentDescription = null,
                                        tint = if (event.type == CalendarEventTypeEnum.SHIFT) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    viewModel.onDateSelected(uiState.selectedDate)
                                    onNavigate(AppScreenEnum.CALENDAR_DAY)
                                }
                            )
                        }
                    }
                }
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
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(2.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            if (summary != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    summary.typeCounts.forEach { (type, count) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 1.dp)
                            )
                            Icon(
                                imageVector = if (type == CalendarEventTypeEnum.SHIFT) Icons.Default.Work else Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = if (type == CalendarEventTypeEnum.SHIFT) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MonthSelector(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, null)
        }
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
