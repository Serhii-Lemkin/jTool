package com.jtool.android.core.models.common.enums

import androidx.annotation.Keep

@Keep
enum class NotificationTypeEnum {
    INFO,
    INVITE,
    JOIN_REQUEST,
    GROUP_JOIN_ACCEPTED,
    GROUP_JOIN_DECLINED,
    EVENT_ACCEPTED,
    EVENT_DECLINED,
    EVENT_COMMENT,
    EVENT_DELETED,
    SUPPORT_STATUS_CHANGED,
    SUPPORT_COMMENT_ADDED
}
