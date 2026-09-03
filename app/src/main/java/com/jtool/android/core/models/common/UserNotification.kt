package com.jtool.android.core.models.common

import androidx.annotation.Keep
import com.jtool.android.core.models.common.enums.NotificationTypeEnum

@Keep
data class UserNotification(
    val id: String = "",
    val groupId: String? = null,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val type: NotificationTypeEnum = NotificationTypeEnum.INFO,
    val isGroupLevel: Boolean = false
)
