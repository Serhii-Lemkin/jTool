package com.jtool.android.features.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.core.models.tracker.TrackerRecord
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.models.tracker.enums.TrackerTimeFilterEnum
import com.jtool.android.features.tracker.components.add_record_sheet.AddRecordSheet
import com.jtool.android.features.tracker.components.tracker_chart.TrendifyChart
import com.jtool.android.features.tracker.components.tracker_chart.ChartSeries
import com.jtool.android.features.tracker.components.tracker_filter_sheet.TrackerFilterSheet
import com.jtool.android.features.tracker.components.tracker_details.MemberPickerSheet
import com.jtool.android.features.tracker.components.tracker_details.MetricPickerSheet
import com.jtool.android.features.tracker.components.delete_record_confirm_sheet.DeleteRecordConfirmSheet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDetailsScreen(
    dataManager: DataManager,
    authManager: com.jtool.android.core.auth.AuthManager,
    settingsManager: com.jtool.android.core.settings.SettingsManager,
    onBack: () -> Unit,
    viewModel: TrackerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val tracker = uiState.currentTracker ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var trackerToAddRecord by remember { mutableStateOf<Tracker?>(null) }
    
    var recordToEdit by remember { mutableStateOf<TrackerRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            GenericTitle(
                title = tracker.title,
                onBack = onBack,
                isLoading = uiState.isLoading,
                onRefresh = { viewModel.onEvent(TrackerEvent.Refresh) },
                actions = {
                    TitleActionIcon(
                        icon = Icons.Default.FilterList,
                        onClick = { showFilterSheet = true },
                        tint = if (uiState.timeFilter != TrackerTimeFilterEnum.ALL) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            )
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { trackerToAddRecord = tracker },
                contentDescription = Loc.Tracker.addRecord
            )
        }
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.onEvent(TrackerEvent.Refresh) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            TrackerDetailsContent(
                tracker = tracker,
                records = uiState.records,
                userNames = uiState.userNames,
                currentUserId = authManager.getCurrentUser()?.uid ?: "",
                timeFilter = uiState.timeFilter,
                onEditRecord = { recordToEdit = it },
                onDeleteRecord = { recordToDelete = it },
                onEvent = { viewModel.onEvent(it) }
            )
        }
    }

    if (showFilterSheet) {
        TrackerFilterSheet(
            currentFilter = uiState.timeFilter,
            onFilterSelected = { viewModel.onEvent(TrackerEvent.SetTimeFilter(it)) },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (trackerToAddRecord != null) {
        AddRecordSheet(
            tracker = trackerToAddRecord!!,
            onDismiss = { trackerToAddRecord = null },
            onAdd = { viewModel.onEvent(TrackerEvent.AddRecord(it)) },
            userId = authManager.getCurrentUser()?.uid ?: "",
            userName = settingsManager.getUserName()
        )
    }

    if (recordToEdit != null) {
        AddRecordSheet(
            tracker = tracker,
            onDismiss = { recordToEdit = null },
            onAdd = { viewModel.onEvent(TrackerEvent.UpdateRecord(it)) },
            userId = authManager.getCurrentUser()?.uid ?: "",
            userName = settingsManager.getUserName(),
            editingRecord = recordToEdit
        )
    }

    if (recordToDelete != null) {
        DeleteRecordConfirmSheet(
            onDismiss = { recordToDelete = null },
            onConfirm = {
                viewModel.onEvent(TrackerEvent.DeleteRecord(recordToDelete!!))
                recordToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackerDetailsContent(
    tracker: Tracker,
    records: List<TrackerRecord>,
    userNames: Map<String, String>,
    currentUserId: String,
    timeFilter: TrackerTimeFilterEnum,
    onEditRecord: (TrackerRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onEvent: (TrackerEvent) -> Unit
) {
    var selectedMetricId by remember { mutableStateOf<String?>(null) }
    var selectedUserIds by remember { mutableStateOf(setOf(currentUserId)) }
    
    var showMemberPicker by remember { mutableStateOf(false) }
    var showMetricPicker by remember { mutableStateOf(false) }

    val allUserIds = remember(records) { records.map { it.userId }.distinct() }

    val metricColors = remember {
        listOf(
            Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04),
            Color(0xFF34A853), Color(0xFF8E24AA), Color(0xFFF06292), Color(0xFF4DB6AC)
        )
    }

    val filteredRecords = remember(records, selectedUserIds, timeFilter) {
        val now = System.currentTimeMillis()
        val timeCutoff = when (timeFilter) {
            TrackerTimeFilterEnum.ALL -> 0L
            TrackerTimeFilterEnum.LAST_DAY -> now - (24 * 60 * 60 * 1000L)
            TrackerTimeFilterEnum.LAST_MONTH -> now - (30 * 24 * 60 * 60 * 1000L)
            TrackerTimeFilterEnum.LAST_3_MONTHS -> now - (90 * 24 * 60 * 60 * 1000L)
            TrackerTimeFilterEnum.LAST_6_MONTHS -> now - (180 * 24 * 60 * 60 * 1000L)
            TrackerTimeFilterEnum.LAST_YEAR -> now - (365 * 24 * 60 * 60 * 1000L)
        }

        records.filter { it.timestamp >= timeCutoff && it.userId in selectedUserIds }
    }

    val globalTimestamps = remember(filteredRecords) {
        filteredRecords.map { it.timestamp }.distinct().sorted()
    }

    val chartSeries = remember(filteredRecords, selectedMetricId, tracker.metrics, selectedUserIds, globalTimestamps, tracker.userColors) {
        if (selectedMetricId != null) {
            // Compare users for a single metric
            selectedUserIds.mapIndexed { index, uid ->
                val points = globalTimestamps.map { ts ->
                    filteredRecords.find { it.timestamp == ts && it.userId == uid }?.values?.get(selectedMetricId!!)
                }
                val color = tracker.userColors[uid]?.let { Color(it) } ?: metricColors[index % metricColors.size]
                ChartSeries(color, points)
            }
        } else {
            // Show all metrics (aggregated if multiple users)
            tracker.metrics.mapIndexed { index, metric ->
                val points = globalTimestamps.map { ts ->
                    val userRecordsAtTs = filteredRecords.filter { it.timestamp == ts }
                    if (userRecordsAtTs.isEmpty()) null
                    else userRecordsAtTs.sumOf { it.values[metric.id] ?: 0.0 }
                }
                val color = metric.color?.let { Color(it) } ?: metricColors[index % metricColors.size]
                ChartSeries(color, points)
            }
        }
    }

    val tendency = remember(chartSeries) {
        if (chartSeries.size != 1) null
        else {
            val validPoints = chartSeries[0].points.filterNotNull()
            if (validPoints.size < 2) null
            else {
                val last = validPoints.last()
                val prev = validPoints[validPoints.size - 2]
                when {
                    last > prev -> Loc.Tracker.tendencyUp
                    last < prev -> Loc.Tracker.tendencyDown
                    else -> Loc.Tracker.tendencyStable
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // People Picker Button
            FilterChip(
                selected = true,
                onClick = { showMemberPicker = true },
                label = {
                    val label = when {
                        allUserIds.size == 1 -> userNames[allUserIds.first()] ?: Loc.General.unknown
                        selectedUserIds.size == allUserIds.size -> Loc.Tracker.allPeople
                        selectedUserIds.size == 1 -> userNames[selectedUserIds.first()] ?: Loc.General.unknown
                        else -> "${Loc.Tracker.people} (${selectedUserIds.size})"
                    }
                    Text(label)
                },
                leadingIcon = { Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                modifier = Modifier.weight(1f)
            )

            // Metric Picker Button
            FilterChip(
                selected = true,
                onClick = { showMetricPicker = true },
                label = {
                    val metric = tracker.metrics.find { it.id == selectedMetricId }
                    Text(metric?.name ?: Loc.ShopList.all)
                },
                leadingIcon = { Icon(Icons.Default.Leaderboard, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                if (chartSeries.all { s -> s.points.count { it != null } < 1 }) {
                    Text(Loc.Tracker.insufficientData, style = MaterialTheme.typography.bodySmall)
                } else {
                    TrendifyChart(
                        series = chartSeries,
                        timestamps = globalTimestamps,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // Legend
        if (selectedMetricId != null && selectedUserIds.size > 1) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                selectedUserIds.forEachIndexed { index, uid ->
                    val color = tracker.userColors[uid]?.let { Color(it) } ?: metricColors[index % metricColors.size]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(userNames[uid] ?: Loc.General.unknown, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else if (selectedMetricId == null && tracker.metrics.size > 1) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tracker.metrics.forEachIndexed { index, metric ->
                    val color = metric.color?.let { Color(it) } ?: metricColors[index % metricColors.size]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(metric.name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        if (tendency != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${Loc.Tracker.tendency}: ", style = MaterialTheme.typography.bodySmall)
                Text(
                    tendency,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (tendency == Loc.Tracker.tendencyUp) Color.Green else if (tendency == Loc.Tracker.tendencyDown) Color.Red else Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(Loc.Tracker.history, style = MaterialTheme.typography.titleMedium)
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredRecords.reversed(), key = { it.id }) { record ->
                val dateStr = remember(record.timestamp, Loc.lang) {
                    SimpleDateFormat("dd/MM/yy HH:mm", Locale.forLanguageTag(Loc.lang)).format(Date(record.timestamp))
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(dateStr, style = MaterialTheme.typography.labelSmall)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(userNames[record.userId] ?: record.userName, style = MaterialTheme.typography.labelSmall)
                                
                                Box {
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text(Loc.General.edit) },
                                            onClick = {
                                                menuExpanded = false
                                                onEditRecord(record)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(Loc.General.delete, color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                menuExpanded = false
                                                onDeleteRecord(record.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                        )
                                    }
                                }
                            }
                        }
                        record.values.forEach { (metricId, value) ->
                            val metric = tracker.metrics.find { it.id == metricId }
                            val metricColor = tracker.metrics.indexOf(metric).let { if (it >= 0) metricColors[it % metricColors.size] else MaterialTheme.colorScheme.onSurface }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(metric?.name ?: Loc.General.unknown, style = MaterialTheme.typography.bodyMedium, color = metricColor, fontWeight = FontWeight.Bold)
                                Text("$value ${metric?.unit ?: ""}", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showMemberPicker) {
        MemberPickerSheet(
            allUserIds = allUserIds,
            userNames = userNames,
            userColors = tracker.userColors,
            currentUserId = currentUserId,
            selectedUserIds = selectedUserIds,
            onUserSelected = { uid ->
                selectedUserIds = if (uid in selectedUserIds) {
                    if (selectedUserIds.size > 1) selectedUserIds - uid else selectedUserIds
                } else {
                    selectedUserIds + uid
                }
            },
            onSelectAll = {
                selectedUserIds = if (selectedUserIds.size == allUserIds.size) setOf(currentUserId)
                else allUserIds.toSet()
            },
            onDismiss = { showMemberPicker = false }
        )
    }

    if (showMetricPicker) {
        MetricPickerSheet(
            metrics = tracker.metrics,
            selectedMetricId = selectedMetricId,
            onMetricSelected = { selectedMetricId = it },
            onDismiss = { showMetricPicker = false }
        )
    }
}
