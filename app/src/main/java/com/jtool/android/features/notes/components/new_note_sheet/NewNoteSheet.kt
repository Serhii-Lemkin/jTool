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
        title = Loc.General.add
    ) {
        ActionListItem(
            headline = Loc.Notes.textNote,
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { 
                onTypeSelected(NoteTypeEnum.TEXT)
                onDismiss()
            }
        )
        
        ActionListItem(
            headline = Loc.Notes.checklistNote,
            leadingContent = { Icon(Icons.Default.Checklist, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { 
                onTypeSelected(NoteTypeEnum.CHECKLIST)
                onDismiss()
            }
        )
    }
}
