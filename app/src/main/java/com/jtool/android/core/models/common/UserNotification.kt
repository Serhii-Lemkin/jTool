package com.jtool.android.core.models.common

import androidx.annotation.Keep
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserNotification(
    val id: String = "",
    val groupId: String? = null,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val type: NotificationTypeEnum = NotificationTypeEnum.INFO,
    val isGroupLevel: Boolean = false,
    val excludeUid: String? = null,
    val link: String? = null,
    
    // Metadata for Invites and Join Requests
    val targetUid: String? = null,
    val fromEmail: String? = null,
    val targetName: String? = null,
    val targetEmail: String? = null
)
