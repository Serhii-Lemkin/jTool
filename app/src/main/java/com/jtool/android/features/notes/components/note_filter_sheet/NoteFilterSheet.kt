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
        title = Loc.ShopList.filterItems,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            ActionListItem(
                headline = Loc.ShopList.all,
                leadingContent = { Icon(Icons.Default.List, null) },
                trailingContent = {
                    if (currentFilter == null) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = {
                    onSelectFilter(null)
                    onDismiss()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.Notes.textNote,
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                trailingContent = {
                    if (currentFilter == NoteTypeEnum.TEXT) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = {
                    onSelectFilter(NoteTypeEnum.TEXT)
                    onDismiss()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.Notes.checklistNote,
                leadingContent = { Icon(Icons.Default.Checklist, null) },
                trailingContent = {
                    if (currentFilter == NoteTypeEnum.CHECKLIST) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = {
                    onSelectFilter(NoteTypeEnum.CHECKLIST)
                    onDismiss()
                }
            )
        }
    }
}
