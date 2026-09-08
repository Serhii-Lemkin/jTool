package com.jtool.android.core.models.workday.enums

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class WorkModeEnum {
    QUICK_START,
    CALENDAR_EVENT,
    LOG_HOURS, // Deprecated
    SCHEDULE   // Deprecated
}
