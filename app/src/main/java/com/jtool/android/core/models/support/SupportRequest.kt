package com.jtool.android.core.models.support

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum

@Keep
@IgnoreExtraProperties
data class SupportRequest(
    var id: String = "",
    val number: Int = 0,
    val type: SupportTypeEnum = SupportTypeEnum.BUG,
    val status: SupportStatusEnum = SupportStatusEnum.OPENED,
    val text: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Long = 0L,
    val deviceInfo: Map<String, String> = emptyMap(),
    val crashLog: String? = null,
    val comments: List<com.jtool.android.core.models.common.Comment> = emptyList(),
    
    @get:com.google.firebase.firestore.Exclude
    @set:com.google.firebase.firestore.Exclude
    var commentCount: Int = 0,

    @get:com.google.firebase.firestore.Exclude
    val pendingStatus: SupportStatusEnum? = null,
    
    @get:com.google.firebase.firestore.Exclude
    val pendingType: SupportTypeEnum? = null
) {
    val priority: Int
        get() = when (type) {
            SupportTypeEnum.CRASH -> 0
            SupportTypeEnum.BUG -> 1
            SupportTypeEnum.CHANGE -> 2
            SupportTypeEnum.FEATURE -> 3
        }
}
