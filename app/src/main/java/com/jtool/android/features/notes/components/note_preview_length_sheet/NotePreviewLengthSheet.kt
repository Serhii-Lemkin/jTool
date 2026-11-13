package com.jtool.android.features.notes.components.note_preview_length_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

val NOTE_PREVIEW_LINE_OPTIONS = listOf(1, 2, 3, 5, 0)

fun notePreviewLinesLabel(lines: Int): String = when (lines) {
    0 -> Loc.Notes.previewNoLimit
    1 -> Loc.Notes.previewOneLine
    else -> Loc.Notes.previewLines.format(lines)
}

@Composable
fun NotePreviewLengthSheet(
    currentMaxLines: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Notes.previewLength
    ) {
        Text(
            text = Loc.Notes.previewLengthDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NOTE_PREVIEW_LINE_OPTIONS.forEach { option ->
                ActionListItem(
                    headline = notePreviewLinesLabel(option),
                    trailingContent = if (option == currentMaxLines) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    }
                )
            }
        }
    }
}
