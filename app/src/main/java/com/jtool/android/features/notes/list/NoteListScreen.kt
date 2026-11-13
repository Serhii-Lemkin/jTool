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
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.features.notes.components.new_note_sheet.NewNoteSheet
import com.jtool.android.features.notes.components.note_filter_sheet.NoteFilterSheet
import com.jtool.android.features.notes.components.note_action_sheet.NoteActionSheet
import com.jtool.android.features.notes.components.note_preview_length_sheet.NotePreviewLengthSheet
import com.jtool.android.features.notes.components.note_preview_length_sheet.notePreviewLinesLabel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(uiState: NotesUiState, viewModel: NotesViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var showNewNoteSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showPreviewLengthSheet by remember { mutableStateOf(false) }
    var noteForActionSheet by remember { mutableStateOf<NoteItem?>(null) }

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

    if (showPreviewLengthSheet) {
        NotePreviewLengthSheet(
            currentMaxLines = uiState.previewMaxLines,
            onSelect = { viewModel.onEvent(NotesEvent.SetPreviewMaxLines(it)) },
            onDismiss = { showPreviewLengthSheet = false }
        )
    }

    noteForActionSheet?.let { selectedNote ->
        NoteActionSheet(
            note = selectedNote,
            onEdit = { viewModel.onEvent(NotesEvent.OpenNote(selectedNote, true)) },
            onDelete = { viewModel.onEvent(NotesEvent.RequestDelete(selectedNote)) },
            onDismiss = { noteForActionSheet = null }
        )
    }

    BaseScreen(
        title = Loc.Notes.notes,
        isLoading = uiState.isLoading || uiState.isRefreshing,
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

            com.jtool.android.core.components.layout.TitleDropdownMenu {
                ActionListItem(
                    headline = Loc.Notes.previewLength,
                    supportingText = notePreviewLinesLabel(uiState.previewMaxLines),
                    leadingContent = { Icon(Icons.Default.FormatLineSpacing, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { showPreviewLengthSheet = true }
                )
                com.jtool.android.core.components.layout.ActionListItem(
                    headline = com.jtool.android.core.localization.domains.GeneralLoc.learnMore,
                    supportingText = Loc.s("notes_learnMoreSubtitle", "View online guide and feature documentation"),
                    leadingContent = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        val settingsManager = com.jtool.android.core.settings.SettingsManager(context)
                        com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, null, "personal-hub")
                    }
                )
            }
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { showNewNoteSheet = true },
                contentDescription = Loc.General.add
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(uiState.filteredNotes, key = { it.id }) { note ->
                NoteListItem(
                    note = note,
                    userNames = uiState.userNames,
                    previewMaxLines = uiState.previewMaxLines,
                    onOpen = { viewModel.onEvent(NotesEvent.OpenNote(note, false)) },
                    onMore = { noteForActionSheet = note }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }

            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun NoteListItem(
    note: NoteItem,
    userNames: Map<String, String>,
    previewMaxLines: Int,
    onOpen: () -> Unit,
    onMore: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())

    val count = if (note.checklist.isNotEmpty()) note.checklist.size else note.checklistCount
    val contentPreview = if (note.type == NoteTypeEnum.TEXT) {
        note.content
    } else {
        "$count ☑"
    }

    val authorName = userNames[note.lastUpdatedBy] ?: note.lastUpdatedBy
    val dateStr = dateFormat.format(Date(note.timestamp))
    val metadataText = "${Loc.General.by} $authorName • $dateStr"

    ActionListItem(
        onClick = onOpen,
        headline = note.title.ifBlank { Loc.Notes.untitledNote },
        supportingText = contentPreview.ifBlank { null },
        supportingMaxLines = if (previewMaxLines <= 0) Int.MAX_VALUE else previewMaxLines,
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
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, null)
            }
        }
    )
}
