package com.jtool.android.core.models.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.NotificationTypeEnum

val UserNotification.icon: ImageVector
    get() = when (type) {
        NotificationTypeEnum.ACCEPTED -> Icons.Default.CheckCircle
        NotificationTypeEnum.DECLINED -> Icons.Default.Cancel
        NotificationTypeEnum.INVITE -> Icons.Default.Group
        NotificationTypeEnum.JOIN_REQUEST -> Icons.Default.PersonAdd
        NotificationTypeEnum.INFO -> if (isGroupLevel) Icons.Default.Groups else Icons.Default.Info
    }

val UserNotification.typeTitle: String
    get() = when (type) {
        NotificationTypeEnum.INVITE -> Loc.Settings.groupInvitation
        NotificationTypeEnum.JOIN_REQUEST -> Loc.Settings.joinRequests
        else -> title
    }

val UserNotification.displayTitle: String
    get() = when (type) {
        NotificationTypeEnum.INVITE -> Loc.Settings.invitedToJoin.format(targetName ?: "Group")
        NotificationTypeEnum.JOIN_REQUEST -> Loc.Settings.wantsToJoin.format(targetName ?: "User")
        else -> message
    }

val UserNotification.displaySubtitle: String
    get() = when (type) {
        NotificationTypeEnum.INVITE -> Loc.Settings.fromLabel.format(fromEmail ?: "")
        NotificationTypeEnum.JOIN_REQUEST -> targetEmail ?: ""
        else -> ""
    }
