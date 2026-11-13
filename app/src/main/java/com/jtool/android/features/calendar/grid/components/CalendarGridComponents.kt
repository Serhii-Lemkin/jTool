package com.jtool.android.features.calendar.grid.components

import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.calendar.DayEventSummary
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getEventColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as TimeTextStyle

@Composable
fun EventItem(
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
fun MonthHeader(
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
fun DaysOfWeekHeader() {
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
fun CalendarGrid(
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
                        modifier = Modifier.weight(1f).let { base ->
                            if (isFullScreenGrid) base.fillMaxHeight() else base
                        }
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
