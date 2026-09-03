package com.jtool.android.features.settings.groups.components.group_invitation_slip

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInvitationSlip(
    invite: Invite,
    isLoading: Boolean = false,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(text = "Processing Invitation...", style = MaterialTheme.typography.bodyLarge)
            } else {
                Icon(
                    Icons.Default.GroupAdd,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Group Invitation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
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
                    Text("Accept Invitation")
                }
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Decline")
                }
            }
        }
    }
}
