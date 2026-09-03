package com.jtool.android.core.models.group

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Invite(
    var inviteId: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val fromEmail: String = "",
    val inviterUid: String = "",
    val timestamp: Long = 0L
)
