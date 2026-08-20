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
import com.jtool.android.core.models.NoteItem
import com.jtool.android.core.models.NoteType
import com.jtool.android.features.notes.NotesEvent
import com.jtool.android.features.notes.NotesUiState
import com.jtool.android.features.notes.NotesViewModel
import com.jtool.android.core.components.comments.getTotalCommentsCount
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.UnifiedFAB
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(uiState: NotesUiState, viewModel: NotesViewModel, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                GenericTitle(title = Loc.notes_notes, onBack = onBack)
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(uiState.notes.sortedByDescending { it.timestamp }, key = { it.id }) { note ->
                    NoteListItem(note, uiState.userNames, viewModel)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
                
                item {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }

        UnifiedFAB(
            onClick = {},
            dropdownContent = { onDismiss ->
                DropdownMenuItem(
                    text = { Text(Loc.notes_textNote) },
                    onClick = { 
                        onDismiss()
                        viewModel.onEvent(NotesEvent.StartNewNote(NoteType.TEXT)) 
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) }
                )
                DropdownMenuItem(
                    text = { Text(Loc.notes_checklistNote) },
                    onClick = { 
                        onDismiss()
                        viewModel.onEvent(NotesEvent.StartNewNote(NoteType.CHECKLIST)) 
                    },
                    leadingIcon = { Icon(Icons.Default.Checklist, null) }
                )
            }
        )
    }
}

@Composable
fun NoteListItem(note: NoteItem, userNames: Map<String, String>, viewModel: NotesViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.onEvent(NotesEvent.OpenNote(note, false)) }
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (note.type == NoteType.TEXT) Icons.AutoMirrored.Filled.Notes else Icons.Default.Checklist,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            if (note.title.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(Loc.general_update) },
                        onClick = { 
                            viewModel.onEvent(NotesEvent.OpenNote(note, true))
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(Loc.general_delete) },
                        onClick = { 
                            viewModel.onEvent(NotesEvent.RequestDelete(note))
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
        
        val contentPreview = if (note.type == NoteType.TEXT) {
            note.content
        } else {
            note.checklist.joinToString(", ") { it.text }
        }
        
        if (contentPreview.isNotBlank()) {
            Text(
                text = contentPreview,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${Loc.general_by} ${userNames[note.lastUpdatedBy] ?: note.lastUpdatedBy}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                val totalComments = getTotalCommentsCount(note.comments)
                if (totalComments > 0) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = totalComments.toString(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = dateFormat.format(Date(note.timestamp)),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
