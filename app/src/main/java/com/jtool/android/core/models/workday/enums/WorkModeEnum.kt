package com.jtool.android.core.models.workday.enums

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class WorkModeEnum {
    QUICK_START,
    LOG_HOURS,
    SCHEDULE
}
