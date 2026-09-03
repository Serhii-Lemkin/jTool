package com.jtool.android.core.models.group.enums

import androidx.annotation.Keep

@Keep
enum class JoinGroupResultEnum {
    JOINED,
    REQUEST_SENT,
    ALREADY_REQUESTED,
    ALREADY_MEMBER,
    FAILED
}
