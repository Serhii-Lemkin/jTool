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
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.features.notes.components.new_note_sheet.NewNoteSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(uiState: NotesUiState, viewModel: NotesViewModel, onBack: () -> Unit) {
    var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var showNewNoteSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.searchQuery) { draftSearchQuery = uiState.searchQuery }

    if (showNewNoteSheet) {
        NewNoteSheet(
            onDismiss = { showNewNoteSheet = false },
            onTypeSelected = { viewModel.onEvent(NotesEvent.StartNewNote(it)) }
        )
    }

    Scaffold(
        topBar = {
            GenericTitle(
                title = Loc.Notes.notes,
                onBack = onBack,
                isLoading = uiState.isLoading || uiState.isRefreshing,
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
                    
                    var filterExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { filterExpanded = true }) {
                            Icon(Icons.Default.FilterList, null)
                        }
                        DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(Loc.ShopList.all) },
                                onClick = { viewModel.onEvent(NotesEvent.SetFilterType(null)); filterExpanded = false },
                                trailingIcon = { if (uiState.filterType == null) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(Loc.Notes.textNote) },
                                onClick = { viewModel.onEvent(NotesEvent.SetFilterType(NoteTypeEnum.TEXT)); filterExpanded = false },
                                trailingIcon = { if (uiState.filterType == NoteTypeEnum.TEXT) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(Loc.Notes.checklistNote) },
                                onClick = { viewModel.onEvent(NotesEvent.SetFilterType(NoteTypeEnum.CHECKLIST)); filterExpanded = false },
                                trailingIcon = { if (uiState.filterType == NoteTypeEnum.CHECKLIST) Icon(Icons.Default.Check, null) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { showNewNoteSheet = true },
                contentDescription = Loc.General.add
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.onEvent(com.jtool.android.features.notes.NotesEvent.Refresh) },
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

    val contentPreview = if (note.type == NoteTypeEnum.TEXT) {
        note.content
    } else {
        if (note.checklistCount <= 0) {
            ""
        } else {
            "${note.checklistCount} ${if (note.checklistCount == 1) Loc.General.item else Loc.General.items}"
        }
    }

    ActionListItem(
        onClick = { viewModel.onEvent(NotesEvent.OpenNote(note, false)) },
        headline = note.title.ifBlank { Loc.Notes.untitledNote },
        supportingText = contentPreview,
        metadataText = buildString {
            append("${Loc.General.by} ${userNames[note.lastUpdatedBy] ?: note.lastUpdatedBy}")
            append(" • ")
            append(dateFormat.format(Date(note.timestamp)))
            if (note.commentCount > 0) {
                append(" • ")
                append(note.commentCount)
                append(" 💬")
            }
        },
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
