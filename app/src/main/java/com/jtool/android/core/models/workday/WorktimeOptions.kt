package com.jtool.android.core.models.workday

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.jtool.android.core.models.workday.enums.WorkModeEnum
import com.jtool.android.core.models.workday.enums.WorkTrackingPreferenceEnum
import kotlinx.serialization.Serializable

@Keep
@IgnoreExtraProperties
@Serializable
data class WorktimeOptions(
    val lastMode: WorkModeEnum = WorkModeEnum.SCHEDULE,
    val trackingPreference: WorkTrackingPreferenceEnum? = null,
    val logSchedule: Map<String, DayLogHours> = emptyMap(),
    val weeklyStartDay: String = "MONDAY",
    
    // Quick Start session
    val freehandStart: String? = null,
    val freehandEnd: String? = null,
    val freehandDurationHours: Int = 8,
    val freehandDurationMinutes: Int = 0,
    val freehandUseDuration: Boolean = false,
    
    // Log Hours session
    val logHoursSessionStart: String? = null,
    @get:PropertyName("isLogHoursSet")
    @PropertyName("isLogHoursSet")
    val isLogHoursSet: Boolean = false,
    
    // Schedule session
    val scheduleStart: String? = null,
    val scheduleEnd: String? = null,
    
    @get:PropertyName("isSessionActive")
    @PropertyName("isSessionActive")
    val isSessionActive: Boolean = false,
    val activeSessionMode: WorkModeEnum? = null,
    val activeEventId: String? = null,

    @get:PropertyName("isWeeklyAdjustmentActive")
    @PropertyName("isWeeklyAdjustmentActive")
    val isWeeklyAdjustmentActive: Boolean = false,
    val weeklyTodayStart: String? = null,
    val weeklyTodayEnd: String? = null,

    val useStableApproximateHours: Boolean = true,
    val stableApproximateStart: String? = null,
    val stableApproximateEnd: String? = null
)
