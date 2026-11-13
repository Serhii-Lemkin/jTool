package com.jtool.android.features.calendar.components

import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.features.calendar.common.CalendarEventHelper
import com.jtool.android.features.calendar.edit.components.event_color_picker_sheet.getEventColor
import java.time.Duration
import java.time.LocalTime

@Composable
fun TimelineHourMarkers(hourHeight: Int) {
    Column {
        (0..23).forEach { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight.dp)
            ) {
                Text(
                    text = String.format("%02d:00", hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 50.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun TimelineEventCard(
    event: CalendarEvent,
    groups: Map<String, GroupInfo>,
    modifier: Modifier = Modifier,
    extension: com.jtool.android.core.models.calendar.CalendarEventExtension? = null,
    onClick: () -> Unit
) {
    val isVirtual = event.id.startsWith("virtual_")
    val isContinuation = event.id.contains("_cont")
    val isInvited = event.currentUserStatus == CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
    
    val start = CalendarEventHelper.parseTime(event.startTime)
    val end = if (event.endTime.isEmpty()) start.plusHours(1) else CalendarEventHelper.parseTime(event.endTime)
    val durationMinutes = if (end.isBefore(start)) {
        Duration.between(start, LocalTime.MAX).toMinutes() + 1
    } else {
        Duration.between(start, end).toMinutes()
    }
    val isSmall = durationMinutes <= 30

    val themeColor = getEventColor(event.colorKey)
    val containerColor = if (isInvited) Color.Transparent else (if (isVirtual) themeColor.copy(alpha = 0.7f) else themeColor)
    val contentColor = Color.White

    val sharedGroupIds = extension?.sharedGroupIds ?: emptyList()

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .alpha(if (isInvited) 0.6f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (isVirtual || isInvited) BorderStroke(1.dp, themeColor) else null
    ) {
        if (event.allDay) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = contentColor
                    )
                    Text(
                        text = event.title.ifEmpty { Loc.Calendar.calendar },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (event.currentUserStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    if (sharedGroupIds.isNotEmpty()) {
                        val groupNames = sharedGroupIds.map { gid ->
                            val group = groups[gid]
                            if (group?.isPrivate == true) Loc.Calendar.private else group?.name ?: gid
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${Loc.Calendar.belongsTo}: ${groupNames.joinToString(", ")}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            val badgeText = if (sharedGroupIds.isNotEmpty()) {
                val groupNames = sharedGroupIds.map { gid ->
                    val group = groups[gid]
                    if (group?.isPrivate == true) Loc.Calendar.private else group?.name ?: gid
                }
                "${Loc.Calendar.belongsTo}: ${groupNames.joinToString(", ")}"
            } else {
                null
            }
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
                            text = event.title.ifEmpty { Loc.Calendar.calendar },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (event.currentUserStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                        if (badgeText != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.25f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (isContinuation) "00:00" else event.startTime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = event.title.ifEmpty { Loc.Calendar.calendar },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (event.currentUserStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) TextDecoration.LineThrough else null
                    )
                    if (badgeText != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
