package com.jtool.android.core.models.support

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class SupportConfig(
    val max_requests: Long = 2L,
    val privacy: String = "",
    val website: String = ""
) {
    companion object {
        const val COLLECTION_NAME = "support_requests"
        const val APP_OPTIONS_COLLECTION = "app_options"
        const val CONFIG_DOCUMENT = "config"
        const val SUPPORT_DATA_DOCUMENT = "support_data"
        const val USER_DATA_COLLECTION = "user_data"
        const val USERS_COLLECTION = "users"
        const val COMMENTS_COLLECTION = "comments"
        
        const val DATE_FORMAT = "yyyyMMdd"
        const val ERROR_LIMIT_REACHED = "LIMIT_REACHED"
    }
}
