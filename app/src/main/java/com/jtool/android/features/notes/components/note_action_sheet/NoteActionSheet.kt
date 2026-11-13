package com.jtool.android.features.notes.components.note_action_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.feature.notes.NoteItem
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun NoteActionSheet(
    note: NoteItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = note.title.ifBlank { Loc.Notes.untitledNote }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionListItem(
                headline = Loc.General.edit,
                supportingText = Loc.Notes.editNoteDesc,
                leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    onEdit()
                    onDismiss()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            ActionListItem(
                headline = Loc.General.delete,
                supportingText = Loc.Notes.deleteNoteDesc,
                headlineColor = MaterialTheme.colorScheme.error,
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )
        }
    }
}
