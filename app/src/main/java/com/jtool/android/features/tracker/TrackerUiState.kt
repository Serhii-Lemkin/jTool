package com.jtool.android.features.tracker

import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.core.models.tracker.TrackerRecord
import com.jtool.android.core.models.tracker.enums.TrackerTimeFilterEnum

data class TrackerUiState(
    val trackers: List<Tracker> = emptyList(),
    val currentTracker: Tracker? = null,
    val records: List<TrackerRecord> = emptyList(),
    val isLoading: Boolean = false,
    val userNames: Map<String, String> = emptyMap(),
    val pagingLimit: Int = 15,
    val timeFilter: TrackerTimeFilterEnum = TrackerTimeFilterEnum.ALL
)
