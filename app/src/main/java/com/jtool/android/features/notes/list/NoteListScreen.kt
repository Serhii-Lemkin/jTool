package com.jtool.android.features.notes.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.feature.notes.NoteItem
import com.jtool.android.core.models.feature.notes.enums.NoteTypeEnum
import com.jtool.android.features.notes.NotesEvent
import com.jtool.android.features.notes.NotesUiState
import com.jtool.android.features.notes.NotesViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.features.notes.components.new_note_sheet.NewNoteSheet
import com.jtool.android.features.notes.components.note_filter_sheet.NoteFilterSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(uiState: NotesUiState, viewModel: NotesViewModel, onBack: () -> Unit) {
    var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var showNewNoteSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.searchQuery) { draftSearchQuery = uiState.searchQuery }

    if (showNewNoteSheet) {
        NewNoteSheet(
            onDismiss = { showNewNoteSheet = false },
            onTypeSelected = { viewModel.onEvent(NotesEvent.StartNewNote(it)) }
        )
    }

    if (showFilterSheet) {
        NoteFilterSheet(
            currentFilter = uiState.filterType,
            onSelectFilter = { viewModel.onEvent(NotesEvent.SetFilterType(it)) },
            onDismiss = { showFilterSheet = false }
        )
    }

    BaseScreen(
        title = Loc.Notes.notes,
        isLoading = false,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(NotesEvent.Refresh) },
        isSearchVisible = uiState.isSearchVisible,
        searchQuery = draftSearchQuery,
        onSearchQueryChange = { draftSearchQuery = it },
        onSearchClose = {
            viewModel.onEvent(NotesEvent.UpdateSearchQuery(""))
            viewModel.onEvent(NotesEvent.ToggleSearch)
        },
        onSearchTriggered = {
            viewModel.onEvent(NotesEvent.UpdateSearchQuery(draftSearchQuery))
        },
        actions = {
            IconButton(onClick = { viewModel.onEvent(NotesEvent.ToggleSearch) }) {
                Icon(Icons.Default.Search, null)
            }
            
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Default.FilterList, null)
            }
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { showNewNoteSheet = true },
                contentDescription = Loc.General.add
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading || uiState.isRefreshing,
            onRefresh = { viewModel.onEvent(NotesEvent.Refresh) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(uiState.filteredNotes, key = { it.id }) { note ->
                    NoteListItem(note, uiState.userNames, viewModel)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
                
                item {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun NoteListItem(note: NoteItem, userNames: Map<String, String>, viewModel: NotesViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())

    val count = if (note.checklist.isNotEmpty()) note.checklist.size else note.checklistCount
    val contentPreview = if (note.type == NoteTypeEnum.TEXT) {
        note.content
    } else {
        "$count ${if (count == 1) Loc.General.item else Loc.General.items}"
    }

    val authorName = userNames[note.lastUpdatedBy] ?: note.lastUpdatedBy
    val dateStr = dateFormat.format(Date(note.timestamp))
    val metadataText = "${Loc.General.by} $authorName • $dateStr"

    ActionListItem(
        onClick = { viewModel.onEvent(NotesEvent.OpenNote(note, false)) },
        headline = note.title.ifBlank { Loc.Notes.untitledNote },
        supportingText = contentPreview,
        metadataText = metadataText,
        leadingContent = {
            Icon(
                imageVector = if (note.type == NoteTypeEnum.TEXT) Icons.AutoMirrored.Filled.Notes else Icons.Default.Checklist,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(Loc.General.edit) },
                        onClick = { 
                            viewModel.onEvent(NotesEvent.OpenNote(note, true))
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Loc.General.delete) },
                        onClick = { 
                            viewModel.onEvent(NotesEvent.RequestDelete(note))
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    )
}
