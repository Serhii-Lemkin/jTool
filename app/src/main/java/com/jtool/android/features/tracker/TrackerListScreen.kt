package com.jtool.android.features.tracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.features.tracker.components.create_tracker_sheet.CreateTrackerSheet
import com.jtool.android.features.tracker.components.delete_tracker_confirm_sheet.DeleteTrackerConfirmSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerListScreen(
    currentGroupId: String?,
    onBack: () -> Unit,
    onNavigate: (AppScreenEnum) -> Unit,
    viewModel: TrackerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingTracker by remember { mutableStateOf<Tracker?>(null) }
    var trackerToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentGroupId) {
        viewModel.setGroupId(currentGroupId)
    }

    BaseScreen(
        title = Loc.Tracker.trendify,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(TrackerEvent.Refresh) },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { showCreateSheet = true },
                contentDescription = Loc.Tracker.createTracker
            )
        }
    ) { _ ->
        if (uiState.isLoading) {
            // Keep empty while loading
        } else if (uiState.trackers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Loc.Tracker.noTrackers, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.trackers, key = { it.id }) { tracker ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    ActionListItem(
                        onClick = {
                            viewModel.onEvent(TrackerEvent.SelectTracker(tracker))
                            onNavigate(AppScreenEnum.TRENDIFY_DETAILS)
                        },
                        headline = tracker.title,
                        supportingText = tracker.metrics.joinToString(" • ") { "${it.name} (${it.unit})" },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(Loc.General.edit) },
                                        onClick = {
                                            editingTracker = tracker
                                            menuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(Loc.General.delete, color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            trackerToDelete = tracker.id
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    if (showCreateSheet) {
        CreateTrackerSheet(
            onDismiss = { showCreateSheet = false },
            onConfirm = {
                viewModel.onEvent(TrackerEvent.CreateTracker(it))
                showCreateSheet = false
            }
        )
    }

    if (editingTracker != null) {
        CreateTrackerSheet(
            editingTracker = editingTracker,
            onDismiss = { editingTracker = null },
            onConfirm = {
                viewModel.onEvent(TrackerEvent.UpdateTracker(it))
                editingTracker = null
            }
        )
    }

    if (trackerToDelete != null) {
        DeleteTrackerConfirmSheet(
            onDismiss = { trackerToDelete = null },
            onConfirm = {
                viewModel.onEvent(TrackerEvent.DeleteTracker(trackerToDelete!!))
                trackerToDelete = null
            }
        )
    }
}
