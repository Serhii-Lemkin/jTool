package com.jtool.android.features.calendar.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import java.util.Locale

@Composable
fun TimelineHourMarkers(hourHeight: Int) {
    Column(Modifier.fillMaxWidth()) {
        (0..23).forEach { hour ->
            Box(
                modifier = Modifier
                    .height(hourHeight.dp)
                    .fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.TopCenter),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = String.format(Locale.ENGLISH, "%02d:00", hour),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TimelineEventCard(
    event: CalendarEvent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isVirtual = event.id.startsWith("virtual_")
    val isShift = event.type == CalendarEventTypeEnum.SHIFT
    val isContinuation = event.id.endsWith("_cont")
    val isOvernight = event.note.startsWith("Ends tomorrow")
    
    val start = com.jtool.android.features.calendar.common.CalendarEventHelper.parseTime(event.startTime)
    val end = if (event.endTime.isNullOrBlank()) start.plusHours(1) else com.jtool.android.features.calendar.common.CalendarEventHelper.parseTime(event.endTime)
    val durationMinutes = if (end.isBefore(start)) {
        java.time.Duration.between(start, java.time.LocalTime.MAX).toMinutes() + 1
    } else {
        java.time.Duration.between(start, end).toMinutes()
    }
    val isSmall = durationMinutes <= 30

    val containerColor = if (isShift) {
        if (isVirtual) Color(0xFFE8F5E9) else Color(0xFFC8E6C9)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isVirtual) 0.5f else 1f)
    }
    
    val contentColor = if (isShift) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (isVirtual) borderStroke(1.dp, contentColor.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = if (isSmall) 0.dp else 4.dp),
            verticalArrangement = if (isSmall) Arrangement.Center else Arrangement.Top
        ) {
            if (isSmall) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isContinuation) "00:00" else event.startTime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = event.title.ifEmpty { if (isShift) Loc.Calendar.shift else Loc.Calendar.calendar },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isShift) Icons.Default.Work else Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isContinuation) "00:00" else event.startTime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (isOvernight || isContinuation) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Text(
                    text = event.title.ifEmpty { if (isShift) Loc.Calendar.shift else Loc.Calendar.calendar },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.sp
                )
                
                if (event.note.isNotEmpty()) {
                    Text(
                        text = event.note,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(0.7f),
                        lineHeight = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
