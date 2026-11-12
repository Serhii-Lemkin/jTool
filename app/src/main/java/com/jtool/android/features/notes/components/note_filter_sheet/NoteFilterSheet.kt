package com.jtool.android.features.notes.components.note_filter_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.feature.notes.enums.NoteTypeEnum
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun NoteFilterSheet(
    currentFilter: NoteTypeEnum?,
    onSelectFilter: (NoteTypeEnum?) -> Unit,
    onDismiss: () -> Unit
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
                headline = Loc.ShopList.all,
                supportingText = Loc.s("notes_filterAllDesc", "Show all text notes and checklists"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = if (currentFilter == null) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {
                    onSelectFilter(null)
                    onDismiss()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.Notes.textNote,
                supportingText = Loc.s("notes_filterTextDesc", "Show text notes only"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = if (currentFilter == NoteTypeEnum.TEXT) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {
                    onSelectFilter(NoteTypeEnum.TEXT)
                    onDismiss()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.Notes.checklistNote,
                supportingText = Loc.s("notes_filterChecklistDesc", "Show checklist notes only"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = if (currentFilter == NoteTypeEnum.CHECKLIST) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {
                    onSelectFilter(NoteTypeEnum.CHECKLIST)
                    onDismiss()
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
