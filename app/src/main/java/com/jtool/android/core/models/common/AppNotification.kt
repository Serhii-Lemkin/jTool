package com.jtool.android.core.models.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.NotificationTypeEnum

val UserNotification.icon: ImageVector
    get() = when (type) {
        NotificationTypeEnum.EVENT_ACCEPTED, NotificationTypeEnum.GROUP_JOIN_ACCEPTED -> Icons.Default.CheckCircle
        NotificationTypeEnum.EVENT_DECLINED, NotificationTypeEnum.EVENT_DELETED, NotificationTypeEnum.GROUP_JOIN_DECLINED -> Icons.Default.Cancel
        NotificationTypeEnum.EVENT_COMMENT, NotificationTypeEnum.SUPPORT_COMMENT_ADDED -> Icons.AutoMirrored.Filled.Comment
        NotificationTypeEnum.SUPPORT_STATUS_CHANGED -> Icons.Default.ConfirmationNumber
        NotificationTypeEnum.INVITE -> Icons.Default.Group
        NotificationTypeEnum.JOIN_REQUEST -> Icons.Default.PersonAdd
        NotificationTypeEnum.INFO -> if (isGroupLevel) Icons.Default.Groups else Icons.Default.Info
    }

val UserNotification.typeTitle: String
    get() = when {
        type == NotificationTypeEnum.INVITE -> Loc.Settings.groupInvitation
        type == NotificationTypeEnum.JOIN_REQUEST -> Loc.Settings.joinRequests
        !titleKey.isNullOrEmpty() -> Loc.s(titleKey, title)
        else -> title
    }

val UserNotification.displayTitle: String
    get() = when {
        type == NotificationTypeEnum.INVITE -> Loc.Settings.invitedToJoin.format(targetName ?: "Group")
        type == NotificationTypeEnum.JOIN_REQUEST -> Loc.Settings.wantsToJoin.format(targetName ?: "User")
        !messageKey.isNullOrEmpty() -> {
            val template = Loc.s(messageKey ?: "", message)
            if (messageArgs.isNotEmpty()) {
                try {
                    val typedArgs: Array<Any> = messageArgs.map { arg ->
                        (arg.toIntOrNull() ?: arg) as Any
                    }.toTypedArray()
                    template.format(*typedArgs)
                } catch (_: Exception) {
                    template
                }
            } else template
        }
        else -> message
    }

val UserNotification.displaySubtitle: String
    get() = when (type) {
        NotificationTypeEnum.INVITE -> Loc.Settings.fromLabel.format(fromEmail ?: "")
        NotificationTypeEnum.JOIN_REQUEST -> targetEmail ?: ""
        else -> ""
    }
