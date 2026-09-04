package com.jtool.android.features.settings.groups.components.group_invitation_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.Invite
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun GroupInvitationSheet(
    invite: Invite,
    isLoading: Boolean = false,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = if (isLoading) null else Loc.Settings.groupInvitation,
        showCloseButton = false
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(text = Loc.Settings.processingInvitation, style = MaterialTheme.typography.bodyLarge)
        } else {
            Icon(
                Icons.Default.GroupAdd,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "${invite.fromEmail} invited you to join \"${invite.groupName}\"",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(Loc.Settings.acceptInvitation)
            }
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(Loc.Settings.decline)
            }
        }
    }
}
