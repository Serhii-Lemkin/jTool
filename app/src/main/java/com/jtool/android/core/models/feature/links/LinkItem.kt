package com.jtool.android.core.models.feature.links

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class LinkItem(
    var id: String = "",
    val title: String = "",
    val url: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = ""
)
