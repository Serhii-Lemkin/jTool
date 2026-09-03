package com.jtool.android.features.calendar.day

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val hourHeight = 90 // dp
    
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            kotlinx.coroutines.delay(60_000) // Update every minute
        }
    }

    LaunchedEffect(uiState.selectedDate) {
        viewModel.refresh()
        // Scroll to 8 AM by default
        val scrollPos = with(density) { (8 * hourHeight).dp.toPx() }
        scrollState.scrollTo(scrollPos.toInt())
    }

    if (uiState.showAddEditDialog) {
        AddEditEventDialog(
            event = uiState.editingEvent,
            selectedDate = uiState.selectedDate,
            onDismiss = { viewModel.toggleAddEditDialog(false) },
            onConfirm = { event, isShared -> 
                viewModel.addEvent(event, isShared)
                viewModel.toggleAddEditDialog(false)
            }
        )
    }

    Scaffold(
        topBar = {
            GenericTitle(
                title = uiState.selectedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                onBack = onBack,
                actions = {
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
                contentDescription = Loc.calendar_addEvent
            )
        }
    ) { padding ->
        val overlapGroups = remember(uiState.privateEvents) {
            CalendarEventHelper.calculateOverlapGroups(uiState.privateEvents)
        }

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            TimelineHourMarkers(hourHeight)

            if (uiState.selectedDate == java.time.LocalDate.now()) {
                val timeTop = (currentTime.hour * hourHeight) + (currentTime.minute * hourHeight / 60)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = timeTop.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.error, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.error,
                        thickness = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp) // Offset for time text
            ) {
                val containerWidth = maxWidth

                uiState.privateEvents.forEach { event ->
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
                                onNavigate(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                            }
                        )
                    }
                }
            }
        }
    }
}
