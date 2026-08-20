package com.jtool.android.features.support.logic

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
enum class SupportType {
    BUG, CRASH, FEATURE, CHANGE
}

@Keep
enum class SupportStatus {
    OPENED, IN_PROGRESS, CLOSED, CUT
}

@Keep
@IgnoreExtraProperties
data class SupportRequest(
    val id: String = "",
    val number: Int = 0,
    val type: SupportType = SupportType.BUG,
    val status: SupportStatus = SupportStatus.OPENED,
    val text: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Long = 0L,
    val deviceInfo: Map<String, String> = emptyList<Pair<String, String>>().toMap(),
    val crashLog: String? = null,
    val comments: List<com.jtool.android.core.models.Comment> = emptyList()
) {
    val priority: Int
        get() = when (type) {
            SupportType.CRASH -> 0
            SupportType.BUG -> 1
            SupportType.CHANGE -> 2
            SupportType.FEATURE -> 3
        }
}
