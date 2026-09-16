package com.jtool.android.features.links.components.link_action_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun LinkActionSheet(
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Links.links,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            ActionListItem(
                headline = Loc.Links.copy,
                leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                onClick = {
                    onCopy()
                    onDismiss()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.General.edit,
                leadingContent = { Icon(Icons.Default.Edit, null) },
                onClick = {
                    onEdit()
                    onDismiss()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.General.delete,
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
