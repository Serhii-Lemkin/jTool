package com.jtool.android.features.notes.components.title_suggestion_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun TitleSuggestionSheet(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Notes.addTitleQuestion,
        showCloseButton = false
    ) {
        Text(
            text = Loc.Notes.addTitleExpl,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text(Loc.Notes.noteTitlePlaceholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(Loc.General.cancel)
            }
            
            Button(
                onClick = { onConfirm(titleInput) },
                modifier = Modifier.weight(1f)
            ) {
                Text(Loc.General.save)
            }
        }
    }
}
