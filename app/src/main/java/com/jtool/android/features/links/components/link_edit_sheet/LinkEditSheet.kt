package com.jtool.android.features.links.components.link_edit_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun LinkEditSheet(
    title: String,
    url: String,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Links.addLink,
        showCloseButton = false
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(Loc.Links.linkTitle) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text(Loc.Links.linkUrl) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(Loc.Links.urlHint) }
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onConfirm,
            enabled = title.isNotBlank() && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(Loc.General.add)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(Loc.General.cancel)
        }
    }
}
