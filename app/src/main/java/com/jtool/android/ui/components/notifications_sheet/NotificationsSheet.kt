package com.jtool.android.ui.components.notifications_sheet

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.jtool.android.core.models.common.icon
import com.jtool.android.core.models.common.typeTitle
import com.jtool.android.core.models.common.displayTitle
import com.jtool.android.core.models.common.displaySubtitle
import com.jtool.android.ui.components.common.GenericSheet
import com.jtool.android.core.components.layout.ActionListItem

@Composable
fun NotificationsSheet(
    notifications: List<UserNotification>,
    onAcceptInvite: (UserNotification) -> Unit,
    onDeclineInvite: (UserNotification) -> Unit,
    onAcceptJoinRequest: (UserNotification) -> Unit,
    onDeclineJoinRequest: (UserNotification) -> Unit,
    onDismissNotification: (UserNotification) -> Unit,
    onShowError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.General.notifications,
        showCloseButton = false
    ) {
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Loc.Settings.noNotifications,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.Top
            ) {
                items(notifications, key = { it.id + it.timestamp }) { notification ->
                    AppNotificationItem(
                        notification = notification,
                        onAcceptInvite = onAcceptInvite,
                        onDeclineInvite = onDeclineInvite,
                        onAcceptJoinRequest = onAcceptJoinRequest,
                        onDeclineJoinRequest = onDeclineJoinRequest,
                        onDismissNotification = onDismissNotification,
                        onShowError = onShowError
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun AppNotificationItem(
    notification: UserNotification,
    onAcceptInvite: (UserNotification) -> Unit,
    onDeclineInvite: (UserNotification) -> Unit,
    onAcceptJoinRequest: (UserNotification) -> Unit,
    onDeclineJoinRequest: (UserNotification) -> Unit,
    onDismissNotification: (UserNotification) -> Unit,
    onShowError: (String) -> Unit
) {
    val context = LocalContext.current

    ActionListItem(
        onClick = if (notification.link != null) {
            {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notification.link))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    onShowError(Loc.Links.invalidUrl)
                }
            }
        } else null,
        headline = notification.typeTitle,
        supportingText = notification.displayTitle,
        metadataText = notification.displaySubtitle,
        leadingContent = {
            Icon(
                imageVector = notification.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (notification.link != null) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link",
                        modifier = Modifier.size(18.dp).padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }

                when (notification.type) {
                    NotificationTypeEnum.INVITE -> {
                        IconButton(onClick = { onDeclineInvite(notification) }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { onAcceptInvite(notification) }) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    NotificationTypeEnum.JOIN_REQUEST -> {
                        IconButton(onClick = { onDeclineJoinRequest(notification) }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { onAcceptJoinRequest(notification) }) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    else -> {
                        IconButton(onClick = { 
                            android.util.Log.d("jTool", "User pressed dismiss on notification: ${notification.id}")
                            onDismissNotification(notification) 
                        }) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    )
}
