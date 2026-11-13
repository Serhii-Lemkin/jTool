package com.jtool.android.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.jtool.android.BuildConfig
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.jtool.android.core.models.group.Invite
import com.jtool.android.core.models.group.JoinRequest
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.ImportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

object MainIntentHelper {
    fun buildUserNotifications(
        invites: List<Invite>,
        joinRequests: List<JoinRequest>,
        personalNotifications: List<UserNotification>,
        groupNotifications: List<UserNotification>,
        currentUid: String?
    ): List<UserNotification> {
        val list = mutableListOf<UserNotification>()
        invites.forEach { invite ->
            list.add(
                UserNotification(
                    id = invite.inviteId,
                    groupId = invite.groupId,
                    timestamp = invite.timestamp,
                    type = NotificationTypeEnum.INVITE,
                    targetName = invite.groupName,
                    fromEmail = invite.fromEmail,
                    titleKey = "settings_groupInvitation",
                    messageKey = "settings_invitedToJoin",
                    messageArgs = listOf(invite.groupName)
                )
            )
        }
        joinRequests.filter { it.userId != currentUid }.forEach { req ->
            list.add(
                UserNotification(
                    id = req.userId,
                    groupId = req.groupId,
                    timestamp = req.timestamp,
                    type = NotificationTypeEnum.JOIN_REQUEST,
                    targetUid = req.userId,
                    targetName = req.userName,
                    targetEmail = req.userEmail,
                    titleKey = "settings_joinRequests",
                    messageKey = "settings_wantsToJoin",
                    messageArgs = listOf(req.userName)
                )
            )
        }
        list.addAll(personalNotifications)
        list.addAll(groupNotifications)
        return list.sortedByDescending { it.timestamp }
    }

    fun initializeLanguage(
        context: Context,
        settingsManager: SettingsManager,
        dataManager: DataManager,
        scope: CoroutineScope
    ) {
        if (!settingsManager.hasLanguageBeenSet()) {
            val systemLocale = Locale.getDefault()
            val systemLangCode = systemLocale.language
            val normalizedCode = when (systemLangCode) {
                "iw" -> "he"
                else -> systemLangCode
            }
            val supportedLanguages = Loc.getAvailableLanguages(context).map { it.locale }
            if (supportedLanguages.contains(normalizedCode)) {
                settingsManager.setLanguage(normalizedCode)
            } else {
                scope.launch {
                    dataManager.logRequestedLanguage(
                        name = systemLocale.displayLanguage,
                        code = systemLangCode
                    )
                }
                settingsManager.setLanguage("en")
            }
        }
    }

    fun handleDeepLink(
        link: String,
        settingsManager: SettingsManager,
        onPendingScreen: (AppScreenEnum) -> Unit,
        onCloseNotifications: () -> Unit
    ) {
        val uri = Uri.parse(link)
        if (uri.host == BuildConfig.DEEP_LINK_HOST) {
            val path = uri.path ?: ""
            if (path.startsWith("/event/")) {
                val eventId = Uri.decode(path.removePrefix("/event/")).trim().trimEnd('/')
                if (eventId.isNotEmpty()) {
                    settingsManager.setSelectedCalendarEventId(eventId)
                    onPendingScreen(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                    onCloseNotifications()
                }
            } else if (path.startsWith("/support/")) {
                val requestId = path.removePrefix("/support/")
                if (requestId.isNotEmpty()) {
                    settingsManager.setSelectedSupportRequestId(requestId)
                    onPendingScreen(AppScreenEnum.SUPPORT_HUB)
                    onCloseNotifications()
                }
            }
        }
    }
    fun handleIntent(
        intent: Intent?,
        settingsManager: SettingsManager,
        onPendingScreen: (AppScreenEnum) -> Unit
    ) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    ImportManager.setPendingSharedUris(listOf(uri))
                } else {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
                    if (!sharedText.isNullOrBlank()) {
                        ImportManager.setPendingSharedText(sharedText, sharedSubject)
                    } else {
                        val mimeType = intent.type ?: "unknown"
                        ImportManager.setPendingUnsupportedShare(
                            mimeType = mimeType,
                            details = "Shared intent action: ACTION_SEND"
                        )
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    ImportManager.setPendingSharedUris(uris)
                } else {
                    val mimeType = intent.type ?: "unknown"
                    ImportManager.setPendingUnsupportedShare(
                        mimeType = mimeType,
                        details = "Shared intent action: ACTION_SEND_MULTIPLE"
                    )
                }
            }
            else -> {
                intent.data?.let { uri ->
                    if ((uri.scheme == "https" || uri.scheme == "http") && uri.host == BuildConfig.DEEP_LINK_HOST) {
                        val path = uri.path ?: ""
                        if (path.startsWith("/event/")) {
                            val eventId = Uri.decode(path.removePrefix("/event/")).trim().trimEnd('/')
                            if (eventId.isNotEmpty()) {
                                settingsManager.setSelectedCalendarEventId(eventId)
                                onPendingScreen(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                            }
                        }
                    } else if (uri.scheme == "jtool" && uri.host == "event") {
                        val eventId = uri.lastPathSegment
                        if (!eventId.isNullOrEmpty()) {
                            settingsManager.setSelectedCalendarEventId(eventId)
                            onPendingScreen(AppScreenEnum.CALENDAR_EVENT_DETAIL)
                        }
                    }
                }
            }
        }
    }
}
