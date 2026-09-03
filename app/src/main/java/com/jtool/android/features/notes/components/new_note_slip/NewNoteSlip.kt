package com.jtool.android.features.notes.components.new_note_slip

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewNoteSlip(
    onDismiss: () -> Unit,
    onTypeSelected: (NoteTypeEnum) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Loc.general_add,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            ActionListItem(
                headline = Loc.notes_textNote,
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { 
                    onTypeSelected(NoteTypeEnum.TEXT)
                    onDismiss()
                }
            )
            
            ActionListItem(
                headline = Loc.notes_checklistNote,
                leadingContent = { Icon(Icons.Default.Checklist, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { 
                    onTypeSelected(NoteTypeEnum.CHECKLIST)
                    onDismiss()
                }
            )
        }
    }
}
