package com.jtool.android.features.vaults.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.enums.FileOpenErrorReasonEnum
import com.jtool.android.features.vaults.common.VaultDocUtils

@Composable
fun VaultListItemRow(
    vault: Vault,
    ownerName: String,
    isOwner: Boolean,
    isPinned: Boolean,
    showDriveButtons: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onOpenError: (FileOpenErrorReasonEnum) -> Unit
) {
    val context = LocalContext.current

    ActionListItem(
        onClick = onClick,
        headline = vault.name,
        supportingText = if (vault.description.isNotBlank()) vault.description else "${Loc.General.by} $ownerName",
        metadataText = if (vault.description.isNotBlank()) "${Loc.General.by} $ownerName" else null,
        leadingContent = {
            Box {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = (-4).dp, y = (-4).dp)
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showDriveButtons && vault.driveFolderUrl.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            VaultDocUtils.openExternalUrlWithReason(context, vault.driveFolderUrl)?.let(onOpenError)
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Drive Folder")
                    }
                }

                if (isOwner) {
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                }
            }
        }
    )
}
