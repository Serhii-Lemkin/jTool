package com.jtool.android.ui.components.notifications_slip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.AppNotification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSlip(
    notifications: List<AppNotification>,
    onAcceptInvite: (AppNotification.GroupInvite) -> Unit,
    onDeclineInvite: (AppNotification.GroupInvite) -> Unit,
    onAcceptJoinRequest: (AppNotification.GroupJoinRequest) -> Unit,
    onDeclineJoinRequest: (AppNotification.GroupJoinRequest) -> Unit,
    onDismissSystemNotification: (AppNotification.SystemResult) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(notifications) {
        if (notifications.isEmpty()) {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Loc.general_notifications,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            if (notifications.isEmpty()) {
                Text(
                    text = Loc.settings_noNotifications,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onAcceptInvite = onAcceptInvite,
                            onDeclineInvite = onDeclineInvite,
                            onAcceptJoinRequest = onAcceptJoinRequest,
                            onDeclineJoinRequest = onDeclineJoinRequest,
                            onDismissSystemNotification = onDismissSystemNotification
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(Loc.general_close)
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: AppNotification,
    onAcceptInvite: (AppNotification.GroupInvite) -> Unit,
    onDeclineInvite: (AppNotification.GroupInvite) -> Unit,
    onAcceptJoinRequest: (AppNotification.GroupJoinRequest) -> Unit,
    onDeclineJoinRequest: (AppNotification.GroupJoinRequest) -> Unit,
    onDismissSystemNotification: (AppNotification.SystemResult) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = notification.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.typeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (notification.subtitle.isNotEmpty()) {
                    Text(
                        text = notification.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            if (notification is AppNotification.SystemResult) {
                IconButton(onClick = { onDismissSystemNotification(notification) }) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { 
                            if (notification is AppNotification.GroupInvite) onDeclineInvite(notification)
                            else if (notification is AppNotification.GroupJoinRequest) onDeclineJoinRequest(notification)
                        }
                    ) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(
                        onClick = { 
                            if (notification is AppNotification.GroupInvite) onAcceptInvite(notification)
                            else if (notification is AppNotification.GroupJoinRequest) onAcceptJoinRequest(notification)
                        }
                    ) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
