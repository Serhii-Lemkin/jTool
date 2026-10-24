package com.jtool.android.core.sync.helpers

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.jtool.android.core.config.Config
import com.jtool.android.BuildConfig
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.jtool.android.core.models.user.UserData
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.utils.DeepLinkService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationHelper(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsManager
) {
    private fun getUserDataColl() = firestore.collection("user_data")
    private fun getGroupsColl() = firestore.collection("groups")
    private fun getInvitesColl() = firestore.collection("invites")
    private fun getGroupNotificationsColl(gid: String) = getGroupsColl().document(gid).collection("group_notifications")

    fun personalNotificationsFlow(): Flow<List<UserNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { close(); return@callbackFlow }
        val listener = getUserDataColl().document(uid).collection("notifications").addSnapshotListener { snapshot, error -> 
            if (error != null) {
                Log.e("jTool", "Error in personalNotificationsFlow: ${error.message}")
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserNotification::class.java)?.copy(id = doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun groupNotificationsFlow(groupId: String): Flow<List<UserNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = getGroupNotificationsColl(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("jTool", "Error in groupNotificationsFlow for $groupId: ${error.message}")
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val filtered = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserNotification::class.java)?.copy(id = doc.id, isGroupLevel = true, groupId = groupId)
                }.filter { notif ->
                    if (notif.excludeUid == uid) return@filter false
                    !notif.seenByUids.contains(uid)
                }
                trySend(filtered)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun markNotificationAsRead(notification: UserNotification) {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: ""
        
        try {
            when {
                notification.type == NotificationTypeEnum.INVITE && notification.groupId != null -> {
                    getInvitesColl().document(email).collection("items").document(notification.id).delete().await()
                }
                notification.type == NotificationTypeEnum.JOIN_REQUEST && notification.groupId != null -> {
                    getGroupsColl().document(notification.groupId).collection("join_requests").document(notification.id).delete().await()
                }
                notification.isGroupLevel && notification.groupId != null -> {
                    val notifRef = getGroupNotificationsColl(notification.groupId).document(notification.id)
                    val updatedSeenBy = notification.seenByUids.toMutableList()
                    if (!updatedSeenBy.contains(uid)) {
                        updatedSeenBy.add(uid)
                    }
                    val membersSnapshot = try { getGroupsColl().document(notification.groupId).collection("members").get().await() } catch(_: Exception) { null }
                    val memberUids = membersSnapshot?.documents?.map { it.id }?.toSet() ?: emptySet()

                    if (memberUids.isNotEmpty() && updatedSeenBy.containsAll(memberUids)) {
                        notifRef.delete().await()
                    } else {
                        notifRef.update("seenByUids", updatedSeenBy).await()
                    }
                }
                else -> {
                    if (notification.id.isNotEmpty()) {
                        getUserDataColl().document(uid).collection("notifications").document(notification.id).delete().await()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("jTool", "Failed to mark notification as read", e)
        }
    }

    suspend fun sendEventNotification(groupId: String, eventTitle: String, googleEventId: String) {
        val uid = auth.currentUser?.uid ?: return
        val userName = settings.getUserName()
        val notificationId = UUID.randomUUID().toString()
        val notification = UserNotification(
            id = notificationId,
            groupId = groupId,
            title = Loc.Calendar.eventInvitationNotif,
            message = Loc.Calendar.eventSharedMsg.format(userName, eventTitle),
            timestamp = System.currentTimeMillis(),
            type = NotificationTypeEnum.INFO,
            isGroupLevel = true,
            excludeUid = uid,
            link = DeepLinkService.createEventDeepLink(googleEventId),
            titleKey = "calendar_eventInvitationNotif",
            messageKey = "calendar_eventSharedMsg",
            messageArgs = listOf(userName, eventTitle),
            seenByUids = listOf(uid)
        )
        try {
            val notifRef = getGroupNotificationsColl(groupId).document(notificationId)
            notifRef.set(notification).await()
            Log.d("jTool", "Sent group notification $notificationId to group $groupId")
        } catch (e: Exception) {
            Log.e("jTool", "Failed to send event notification: ${e.message}")
        }
    }

    suspend fun sendEventResponseNotification(groupId: String, eventTitle: String, accepted: Boolean, googleEventId: String) {
        val uid = auth.currentUser?.uid ?: return
        val userName = settings.getUserName()
        val targetType = if (accepted) NotificationTypeEnum.EVENT_ACCEPTED else NotificationTypeEnum.EVENT_DECLINED
        val targetLink = DeepLinkService.createEventDeepLink(googleEventId)
        val titleStr = if (accepted) Loc.Calendar.eventAcceptedNotif else Loc.Calendar.eventDeclinedNotif
        val messageStr = (if (accepted) Loc.Calendar.eventAcceptedMsg else Loc.Calendar.eventDeclinedMsg).format(userName, eventTitle)
        val titleKeyStr = if (accepted) "calendar_eventAcceptedNotif" else "calendar_eventDeclinedNotif"
        val messageKeyStr = if (accepted) "calendar_eventAcceptedMsg" else "calendar_eventDeclinedMsg"

        try {
            val coll = getGroupNotificationsColl(groupId)
            val snapshot = coll.get().await()
            val existingDoc = snapshot.documents.find { doc ->
                val notif = doc.toObject(UserNotification::class.java)
                notif != null && notif.type == targetType && notif.link == targetLink
            }

            if (existingDoc != null) {
                existingDoc.reference.update(mapOf(
                    "message" to messageStr,
                    "messageArgs" to listOf(userName, eventTitle),
                    "timestamp" to System.currentTimeMillis(),
                    "seenByUids" to listOf(uid),
                    "excludeUid" to uid
                )).await()
            } else {
                val notificationId = UUID.randomUUID().toString()
                val notification = UserNotification(
                    id = notificationId,
                    groupId = groupId,
                    title = titleStr,
                    message = messageStr,
                    timestamp = System.currentTimeMillis(),
                    type = targetType,
                    isGroupLevel = true,
                    excludeUid = uid,
                    link = targetLink,
                    titleKey = titleKeyStr,
                    messageKey = messageKeyStr,
                    messageArgs = listOf(userName, eventTitle),
                    seenByUids = listOf(uid)
                )
                coll.document(notificationId).set(notification).await()
            }
        } catch (e: Exception) {
            Log.e("jTool", "Failed to send event response notification: ${e.message}")
        }
    }

    suspend fun sendEventDeletedNotification(groupId: String, eventTitle: String) {
        val uid = auth.currentUser?.uid ?: return
        val userName = settings.getUserName()
        val targetType = NotificationTypeEnum.EVENT_DELETED
        val messageStr = Loc.Calendar.eventDeletedMsg.format(userName, eventTitle)

        try {
            val coll = getGroupNotificationsColl(groupId)
            val snapshot = coll.get().await()
            val existingDoc = snapshot.documents.find { doc ->
                val notif = doc.toObject(UserNotification::class.java)
                notif != null && notif.type == targetType && notif.messageArgs.contains(eventTitle)
            }

            if (existingDoc != null) {
                existingDoc.reference.update(mapOf(
                    "message" to messageStr,
                    "messageArgs" to listOf(userName, eventTitle),
                    "timestamp" to System.currentTimeMillis(),
                    "seenByUids" to listOf(uid),
                    "excludeUid" to uid
                )).await()
            } else {
                val notificationId = UUID.randomUUID().toString()
                val notification = UserNotification(
                    id = notificationId,
                    groupId = groupId,
                    title = Loc.Calendar.eventDeletedNotif,
                    message = messageStr,
                    timestamp = System.currentTimeMillis(),
                    type = targetType,
                    isGroupLevel = true,
                    excludeUid = uid,
                    titleKey = "calendar_eventDeletedNotif",
                    messageKey = "calendar_eventDeletedMsg",
                    messageArgs = listOf(userName, eventTitle),
                    seenByUids = listOf(uid)
                )
                coll.document(notificationId).set(notification).await()
            }
        } catch (e: Exception) {
            Log.e("jTool", "Failed to send event deleted notification: ${e.message}")
        }
    }

    suspend fun sendEventCommentNotification(groupId: String, eventTitle: String, googleEventId: String, commentCount: Int) {
        val uid = auth.currentUser?.uid ?: return
        val targetLink = DeepLinkService.createEventDeepLink(googleEventId)
        val targetType = NotificationTypeEnum.EVENT_COMMENT
        val messageStr = Loc.Calendar.eventCommentMsg.format(commentCount, eventTitle)

        try {
            val coll = getGroupNotificationsColl(groupId)
            val snapshot = coll.get().await()
            val existingDoc = snapshot.documents.find { doc ->
                val notif = doc.toObject(UserNotification::class.java)
                notif != null && notif.type == targetType && notif.link == targetLink
            }

            if (existingDoc != null) {
                existingDoc.reference.update(mapOf(
                    "message" to messageStr,
                    "messageArgs" to listOf(commentCount.toString(), eventTitle),
                    "timestamp" to System.currentTimeMillis(),
                    "seenByUids" to listOf(uid),
                    "excludeUid" to uid
                )).await()
            } else {
                val notificationId = UUID.randomUUID().toString()
                val notification = UserNotification(
                    id = notificationId,
                    groupId = groupId,
                    title = Loc.Calendar.eventCommentNotif,
                    message = messageStr,
                    timestamp = System.currentTimeMillis(),
                    type = targetType,
                    isGroupLevel = true,
                    excludeUid = uid,
                    link = targetLink,
                    titleKey = "calendar_eventCommentNotif",
                    messageKey = "calendar_eventCommentMsg",
                    messageArgs = listOf(commentCount.toString(), eventTitle),
                    seenByUids = listOf(uid)
                )
                coll.document(notificationId).set(notification).await()
            }
        } catch (e: Exception) {
            Log.e("jTool", "Failed to send event comment notification: ${e.message}")
        }
    }

    suspend fun markEventNotificationsAsRead(googleEventId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val userGroupsSnapshot = firestore.collection("users").document(uid).get().await()
            val userGroups = try { userGroupsSnapshot.get("groupIds") as? List<*> } catch(_: Exception) { null }
            
            userGroups?.filterIsInstance<String>()?.forEach { gid ->
                val snapshot = getGroupNotificationsColl(gid).get().await()
                val membersSnapshot = try { getGroupsColl().document(gid).collection("members").get().await() } catch(_: Exception) { null }
                val memberUids = membersSnapshot?.documents?.map { it.id }?.toSet() ?: emptySet()

                snapshot.documents.forEach { doc ->
                    val notif = doc.toObject(UserNotification::class.java)
                    if (notif != null && notif.link?.contains("/event/$googleEventId") == true) {
                        val updatedSeenBy = notif.seenByUids.toMutableList()
                        if (!updatedSeenBy.contains(uid)) {
                            updatedSeenBy.add(uid)
                        }
                        if (memberUids.isNotEmpty() && updatedSeenBy.containsAll(memberUids)) {
                            doc.reference.delete().await()
                        } else {
                            doc.reference.update("seenByUids", updatedSeenBy).await()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("jTool", "Failed to clear event notifications", e)
        }
    }

    suspend fun sendSystemNotification(uid: String, notification: UserNotification) {
        getUserDataColl().document(uid).collection("notifications").document(notification.id).set(notification).await()
    }

    suspend fun sendSupportStatusNotification(targetUid: String, requestId: String, requestNumber: Int, newStatus: com.jtool.android.core.models.support.enums.SupportStatusEnum) {
        val statusText = when (newStatus) {
            com.jtool.android.core.models.support.enums.SupportStatusEnum.OPENED -> Loc.Support.status_opened
            com.jtool.android.core.models.support.enums.SupportStatusEnum.IN_PROGRESS -> Loc.Support.status_in_progress
            com.jtool.android.core.models.support.enums.SupportStatusEnum.CLOSED -> Loc.Support.status_closed
            com.jtool.android.core.models.support.enums.SupportStatusEnum.CUT -> Loc.Support.status_cut
        }
        val notificationId = UUID.randomUUID().toString()
        val notification = UserNotification(
            id = notificationId,
            title = Loc.Support.status_updated_notif,
            message = Loc.Support.status_updated_msg.format(requestNumber.toString(), statusText),
            timestamp = System.currentTimeMillis(),
            type = NotificationTypeEnum.SUPPORT_STATUS_CHANGED,
            isGroupLevel = false,
            link = DeepLinkService.createSupportRequestDeepLink(requestId),
            titleKey = "support_status_updated_notif",
            messageKey = "support_status_updated_msg",
            messageArgs = listOf(requestNumber.toString(), statusText)
        )
        try {
            sendSystemNotification(targetUid, notification)
        } catch (e: Exception) {
            Log.e("jTool", "Failed to send support status notification: ${e.message}")
        }
    }

    suspend fun sendSupportCommentNotification(targetUid: String, requestId: String, requestNumber: Int, commenterName: String) {
        val notificationId = UUID.randomUUID().toString()
        val notification = UserNotification(
            id = notificationId,
            title = Loc.Support.comment_added_notif,
            message = Loc.Support.comment_added_msg.format(commenterName, requestNumber.toString()),
            timestamp = System.currentTimeMillis(),
            type = NotificationTypeEnum.SUPPORT_COMMENT_ADDED,
            isGroupLevel = false,
            link = DeepLinkService.createSupportRequestDeepLink(requestId),
            titleKey = "support_comment_added_notif",
            messageKey = "support_comment_added_msg",
            messageArgs = listOf(commenterName, requestNumber.toString())
        )
        try {
            sendSystemNotification(targetUid, notification)
        } catch (e: Exception) {
            Log.e("jTool", "Failed to send support comment notification: ${e.message}")
        }
    }

    suspend fun sendGroupLevelNotification(groupId: String, notification: UserNotification) {
        getGroupNotificationsColl(groupId).document(notification.id).set(notification).await()
    }
}
