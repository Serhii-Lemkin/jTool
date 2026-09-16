package com.jtool.android.features.notes.edit

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.features.notes.components.title_suggestion_sheet.TitleSuggestionSheet
import com.jtool.android.core.components.comments.CommentSection
import com.jtool.android.core.components.comments.CommentEvent
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.feature.notes.enums.NoteTypeEnum
import com.jtool.android.features.notes.NotesEvent
import com.jtool.android.features.notes.NotesUiState
import com.jtool.android.features.notes.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(uiState: NotesUiState, viewModel: NotesViewModel) {
    var showTitleSheet by remember { mutableStateOf(false) }

    if (showTitleSheet) {
        TitleSuggestionSheet(
            onDismiss = { showTitleSheet = false },
            onConfirm = {
                if (it.isNotBlank()) {
                    viewModel.onEvent(NotesEvent.UpdateDraftTitle(it))
                }
                viewModel.onEvent(NotesEvent.SaveNote)
                showTitleSheet = false
            }
        )
    }

    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            GenericTitle(
                title = "",
                onBack = { viewModel.onEvent(NotesEvent.NavigateBack) },
                isLoading = uiState.isLoading,
                customTitle = {
                    if (uiState.isEditMode) {
                        OutlinedTextField(
                            value = uiState.draftTitle,
                            onValueChange = { viewModel.onEvent(NotesEvent.UpdateDraftTitle(it)) },
                            placeholder = { Text(Loc.Notes.noteTitlePlaceholder) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        )
                    } else {
                        Text(
                            text = if (uiState.draftTitle.isBlank()) Loc.Notes.untitledNote else uiState.draftTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = { 
                            if (uiState.draftTitle.isBlank()) {
                                showTitleSheet = true
                            } else {
                                viewModel.onEvent(NotesEvent.SaveNote)
                            }
                        }) {
                            Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { viewModel.onEvent(NotesEvent.ToggleEditMode) }) {
                            Icon(Icons.Default.Edit, null)
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize().imePadding()
    ) { padding ->
        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                val noteType = uiState.editingNote?.type ?: uiState.creatingNoteType ?: NoteTypeEnum.TEXT
                
                item {
                    if (noteType == NoteTypeEnum.TEXT) {
                        if (uiState.isEditMode) {
                            TextField(
                                value = uiState.draftContent,
                                onValueChange = { viewModel.onEvent(NotesEvent.UpdateDraftContent(it)) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                                placeholder = { Text(Loc.Notes.noteContentHint) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            Text(
                                text = uiState.draftContent,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                            )
                        }
                    }
                }

                if (noteType == NoteTypeEnum.CHECKLIST) {
                    if (uiState.isEditMode) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = uiState.checklistInput,
                                    onValueChange = { viewModel.onEvent(NotesEvent.UpdateChecklistInput(it)) },
                                    placeholder = { Text(Loc.ShopList.addItem) },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                                IconButton(onClick = { viewModel.onEvent(NotesEvent.AddChecklistItem) }) {
                                    Icon(Icons.Default.Add, null)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    val checklist = uiState.editingNote?.checklist ?: emptyList()
                    val realChecklist = checklist.filter { !it.id.startsWith("dummy_") }
                    val sortedChecklist = realChecklist.sortedWith(compareBy<com.jtool.android.core.models.feature.notes.ChecklistItem> { it.isChecked }.thenBy { it.timestamp })
                    
                    items(sortedChecklist, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { viewModel.onEvent(NotesEvent.ToggleChecklistItem(item.id)) },
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Unspecified
                            )
                            if (uiState.isEditMode) {
                                IconButton(onClick = { viewModel.onEvent(NotesEvent.RemoveChecklistItem(item.id)) }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    
                    val currentNote = uiState.editingNote
                    if (currentNote != null && !uiState.isEditMode) {
                        val appUserId = viewModel.getAppUserId()
                        val authUid = viewModel.getAuthUserId()
                        val comments by viewModel.comments.collectAsState(initial = emptyList<com.jtool.android.core.models.common.Comment>())
                        
                        CommentSection(
                            comments = comments,
                            currentUserId = appUserId,
                            userNames = uiState.userNames,
                            onEvent = { event ->
                                when (event) {
                                    is CommentEvent.Add -> viewModel.onEvent(NotesEvent.AddComment(event.content, event.parentId))
                                    is CommentEvent.Edit -> viewModel.onEvent(NotesEvent.EditComment(event.commentId, event.newContent))
                                    is CommentEvent.Delete -> viewModel.onEvent(NotesEvent.DeleteComment(event.commentId))
                                }
                            },
                            isOwner = { it.userId == appUserId || it.userId == authUid }
                        )
                    }
                    
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
