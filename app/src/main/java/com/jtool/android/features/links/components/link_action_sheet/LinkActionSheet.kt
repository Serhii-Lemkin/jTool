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
                headline = Loc.s("links_copyLink", "Copy Link"),
                supportingText = Loc.s("links_copyLinkDesc", "Copy web URL to clipboard"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onCopy()
                    onDismiss()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.General.edit,
                supportingText = Loc.s("links_editLinkDesc", "Edit link title, URL, or category"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onEdit()
                    onDismiss()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.General.delete,
                supportingText = Loc.s("links_deleteLinkDesc", "Remove link permanently"),
                headlineColor = MaterialTheme.colorScheme.error,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
