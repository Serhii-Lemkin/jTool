package com.jtool.android.features.notes.components.new_note_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.feature.notes.enums.NoteTypeEnum
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun NewNoteSheet(
    onDismiss: () -> Unit,
    onTypeSelected: (NoteTypeEnum) -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = null,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionListItem(
                headline = Loc.Notes.textNote,
                supportingText = Loc.s("notes_textNoteDesc", "Create a text note with rich content and comments"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = { 
                    onTypeSelected(NoteTypeEnum.TEXT)
                    onDismiss()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.Notes.checklistNote,
                supportingText = Loc.s("notes_checklistNoteDesc", "Create a note with checkable todo items"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = { 
                    onTypeSelected(NoteTypeEnum.CHECKLIST)
                    onDismiss()
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
