package com.jtool.android.features.tracker

import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.core.models.tracker.TrackerRecord

sealed class TrackerEvent {
    data class CreateTracker(val tracker: Tracker) : TrackerEvent()
    data class UpdateTracker(val tracker: Tracker) : TrackerEvent()
    data class DeleteTracker(val trackerId: String) : TrackerEvent()
    data class AddRecord(val record: TrackerRecord) : TrackerEvent()
    data class UpdateRecord(val record: TrackerRecord) : TrackerEvent()
    data class DeleteRecord(val recordId: String) : TrackerEvent()
    data class SelectTracker(val tracker: Tracker?) : TrackerEvent()
    data class SetTimeFilter(val filter: com.jtool.android.core.models.tracker.enums.TrackerTimeFilterEnum) : TrackerEvent()
    data class UpdateUserColor(val userId: String, val color: Long) : TrackerEvent()
    object LoadMore : TrackerEvent()
    object Refresh : TrackerEvent()
}
