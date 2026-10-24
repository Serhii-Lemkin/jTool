package com.jtool.android.core.utils

import com.jtool.android.BuildConfig

object DeepLinkService {
    fun createEventDeepLink(googleEventId: String): String {
        return "${BuildConfig.DEEP_LINK_BASE_URL}/event/$googleEventId"
    }

    fun createSupportRequestDeepLink(requestId: String): String {
        return "${BuildConfig.DEEP_LINK_BASE_URL}/support/$requestId"
    }

    fun createEventNoteLinkText(googleEventId: String): String {
        val deepLink = createEventDeepLink(googleEventId)
        return "jTool: $deepLink"
    }
}
