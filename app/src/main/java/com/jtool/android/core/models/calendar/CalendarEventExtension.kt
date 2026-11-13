package com.jtool.android.core.models.calendar

import androidx.annotation.Keep
import com.jtool.android.core.models.common.Comment
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CalendarEventExtension(
    val googleEventId: String = "",
    val sharedGroupIds: List<String> = emptyList(),
    val dateTimestamp: Long = 0L,
    val endTimestamp: Long = 0L,
    val localEventId: String = "",
    val comments: List<Comment> = emptyList(),
    val ownerUid: String = "",
    val attendeeStatuses: Map<String, Int> = emptyMap(),
    val colorKey: String? = null,
    val title: String? = null
)
