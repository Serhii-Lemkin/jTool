package com.jtool.android.features.links.components.link_edit_slip

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkEditSlip(
    title: String,
    url: String,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Loc.links_addLink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(Loc.links_linkTitle) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text(Loc.links_linkUrl) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(Loc.links_urlHint) }
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onConfirm,
                enabled = title.isNotBlank() && url.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(Loc.general_add)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(Loc.general_cancel)
            }
        }
    }
}
