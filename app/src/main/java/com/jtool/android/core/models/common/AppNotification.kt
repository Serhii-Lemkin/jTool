package com.jtool.android.core.models.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.Invite
import com.jtool.android.core.models.group.JoinRequest
import com.jtool.android.core.models.common.enums.NotificationTypeEnum

sealed interface AppNotification {
    val id: String
    val timestamp: Long
    val typeTitle: String
    val title: String
    val subtitle: String
    val icon: ImageVector

    data class GroupInvite(
        val invite: Invite
    ) : AppNotification {
        override val id: String = invite.inviteId
        override val timestamp: Long = invite.timestamp
        override val typeTitle: String = Loc.settings_groupInvitation
        override val title: String = Loc.settings_invitedToJoin.format(invite.groupName)
        override val subtitle: String = Loc.settings_fromLabel.format(invite.fromEmail)
        override val icon: ImageVector = Icons.Default.Group
    }

    data class GroupJoinRequest(
        val request: JoinRequest
    ) : AppNotification {
        override val id: String = request.userId
        override val timestamp: Long = request.timestamp
        override val typeTitle: String = Loc.settings_joinRequests
        override val title: String = Loc.settings_wantsToJoin.format(request.userName)
        override val subtitle: String = request.userEmail
        override val icon: ImageVector = Icons.Default.PersonAdd
    }

    data class SystemResult(
        val notification: UserNotification
    ) : AppNotification {
        override val id: String = notification.id
        override val timestamp: Long = notification.timestamp
        override val typeTitle: String = notification.title
        override val title: String = notification.message
        override val subtitle: String = ""
        override val icon: ImageVector = when(notification.type) {
            NotificationTypeEnum.ACCEPTED -> Icons.Default.CheckCircle
            NotificationTypeEnum.DECLINED -> Icons.Default.Cancel
            else -> if (notification.isGroupLevel) Icons.Default.Groups else Icons.Default.Info
        }
    }
}
