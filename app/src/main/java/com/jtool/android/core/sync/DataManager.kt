package com.jtool.android.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.helpers.GroupOperationsHelper
import com.jtool.android.core.sync.helpers.GroupMembershipHelper
import com.jtool.android.core.sync.helpers.GroupInviteHelper
import com.jtool.android.core.sync.helpers.NotificationHelper
import com.jtool.android.core.sync.helpers.SupportDatabaseHelper
import com.jtool.android.core.sync.helpers.UserAdminHelper
import com.jtool.android.core.sync.helpers.SystemDatabaseHelper
import com.jtool.android.core.sync.helpers.WorkdayDatabaseHelper
import com.jtool.android.core.sync.helpers.TrackerDatabaseHelper
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import com.jtool.android.core.sync.helpers.CalendarExtensionDatabaseHelper
import com.jtool.android.core.models.calendar.CalendarEventExtension
import com.jtool.android.core.models.user.PublicProfile
import com.jtool.android.core.models.common.Comment
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.group.Invite
import com.jtool.android.core.models.group.JoinRequest
import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.core.models.tracker.TrackerRecord
import com.jtool.android.core.models.user.User
import com.jtool.android.core.models.workday.WorkHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DataManager(val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val settings = SettingsManager(context)

    private val _optimisticDeletedIds = MutableStateFlow<Set<String>>(emptySet())
    val optimisticDeletedIds = _optimisticDeletedIds.asStateFlow()

    fun recordOptimisticDelete(eventId: String) {
        val current = _optimisticDeletedIds.value
        _optimisticDeletedIds.value = current + eventId
    }

    fun reconcileOptimisticDeletions(currentGoogleIds: Set<String>) {
        val pending = _optimisticDeletedIds.value
        _optimisticDeletedIds.value = pending.intersect(currentGoogleIds)
    }

    fun clearOptimisticDeletions() {
        _optimisticDeletedIds.value = emptySet()
    }

    private val userAdminHelper: UserAdminHelper = UserAdminHelper(firestore, auth, settings) { groupOpsHelper }
    private val notificationHelper: NotificationHelper = NotificationHelper(firestore, auth, settings)
    private val groupOpsHelper: GroupOperationsHelper = GroupOperationsHelper(context, firestore, auth, settings, notificationHelper) { userAdminHelper }
    private val groupMembershipHelper: GroupMembershipHelper = GroupMembershipHelper(firestore, auth, settings, notificationHelper)
    private val groupInviteHelper: GroupInviteHelper = GroupInviteHelper(firestore, auth, settings, notificationHelper) { groupOpsHelper }
    
    private val supportHelper: SupportDatabaseHelper = SupportDatabaseHelper(firestore)
    private val systemHelper: SystemDatabaseHelper = SystemDatabaseHelper(firestore)
    private val workdayHelper: WorkdayDatabaseHelper = WorkdayDatabaseHelper(firestore)
    private val trackerHelper: TrackerDatabaseHelper = TrackerDatabaseHelper(firestore)
    private val calendarExtensionHelper: CalendarExtensionDatabaseHelper = CalendarExtensionDatabaseHelper(firestore)

    fun getSupportColl() = firestore.collection("support_requests")
    suspend fun getAppOptions() = supportHelper.getAppOptions()
    suspend fun submitSupportRequest(request: SupportRequest, uid: String, bypassLimit: Boolean = false) = supportHelper.submitRequest(request, uid, bypassLimit)
    fun supportRequestsFlow(isAdmin: Boolean, userId: String) = supportHelper.getRequestsFlow(isAdmin, userId)
    suspend fun updateSupportStatus(id: String, status: SupportStatusEnum) = supportHelper.updateStatus(id, status)
    suspend fun updateSupportType(id: String, type: SupportTypeEnum) = supportHelper.updateType(id, type)
    fun supportCommentsFlow(id: String) = supportHelper.getCommentsFlow(id)
    suspend fun addSupportComment(id: String, comment: Comment) = supportHelper.addComment(id, comment)
    suspend fun editSupportComment(id: String, commentId: String, newContent: String) = supportHelper.editComment(id, commentId, newContent)
    suspend fun deleteSupportComment(id: String, commentId: String) = supportHelper.deleteComment(id, commentId)
    suspend fun deleteSupportRequest(id: String) = supportHelper.deleteRequest(id)
    suspend fun logRequestedLanguage(name: String, code: String) = systemHelper.logRequestedLanguage(name, code)
    suspend fun addHistoryItem(userId: String, item: WorkHistoryItem) = workdayHelper.addHistoryItem(userId, item)

    fun currentGroupIdFlow(): Flow<String?> = groupOpsHelper.currentGroupIdFlow()
    suspend fun getUserGroups(): List<String> = groupOpsHelper.getUserGroups()
    suspend fun createGroup(groupName: String, isPrivate: Boolean = false): String? = groupOpsHelper.createGroup(groupName, isPrivate)
    suspend fun ensurePrivateGroupExists(): Boolean = groupOpsHelper.ensurePrivateGroupExists()
    suspend fun updateGroupName(groupId: String, newName: String): Boolean = groupOpsHelper.updateGroupName(groupId, newName)
    suspend fun joinGroup(groupId: String, isInviteAcceptance: Boolean = false) = groupOpsHelper.joinGroup(groupId, isInviteAcceptance)
    suspend fun leaveGroup(groupId: String) = groupOpsHelper.leaveGroup(groupId)
    suspend fun switchGroup(groupId: String) = groupOpsHelper.switchGroup(groupId)
    suspend fun updateUserCalendarEmailInGroups(email: String) = groupMembershipHelper.updateUserCalendarEmailInGroups(email)
    fun clearLocalCache() = groupOpsHelper.clearLocalCache()
    suspend fun sendInvite(email: String, groupId: String): Boolean = groupInviteHelper.sendInvite(email, groupId)
    fun invitesFlow(): Flow<List<Invite>> = groupInviteHelper.invitesFlow()
    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean): Boolean = groupInviteHelper.respondToInvite(inviteId, groupId, accept)
    
    suspend fun pushUserSettings(): Boolean = userAdminHelper.pushUserSettings()
    suspend fun fetchUserSettings(force: Boolean = false): Boolean = userAdminHelper.fetchUserSettings(force)
    fun isAdminFlow(): Flow<Boolean> = userAdminHelper.isAdminFlow()
    fun isReviewerFlow(): Flow<Boolean> = userAdminHelper.isReviewerFlow()
    fun allUsersFlow(): Flow<List<User>> = userAdminHelper.allUsersFlow()
    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) = userAdminHelper.setUserAdminStatus(uid, isAdmin)
    suspend fun setUserReviewerStatus(uid: String, isReviewer: Boolean) = userAdminHelper.setUserReviewerStatus(uid, isReviewer)
    suspend fun clearWorkdayOptions(uid: String) = userAdminHelper.clearWorkdayData(uid)
    suspend fun cleanupLegacyWorkHours() = userAdminHelper.cleanupLegacyWorkHours()
    fun allGroupsFlow(): Flow<List<GroupInfo>> = userAdminHelper.allGroupsFlow()
    suspend fun deleteGroupPermanently(groupId: String) = userAdminHelper.deleteGroupPermanently(groupId)
    suspend fun deleteUserPermanently(uid: String) = userAdminHelper.deleteUserPermanently(uid)
    suspend fun deleteMyAccount(): Boolean = userAdminHelper.deleteMyAccount()
    fun userNamesFlow(): Flow<Map<String, String>> = userAdminHelper.userNamesFlow()
    fun publicProfilesFlow(): Flow<Map<String, PublicProfile>> = userAdminHelper.publicProfilesFlow()
    fun groupMembersFlow(groupId: String): Flow<List<String>> = groupMembershipHelper.groupMembersFlow(groupId)
    fun groupMemberEmailsFlow(groupId: String): Flow<List<String>> = groupMembershipHelper.groupMemberEmailsFlow(groupId)
    suspend fun getGroupMemberEmails(groupId: String): List<String> = groupMembershipHelper.getGroupMemberEmails(groupId)
    fun userGroupsFlow(): Flow<List<String>> = groupOpsHelper.userGroupsFlow()
    fun groupInfoFlow(groupIds: List<String>): Flow<Map<String, GroupInfo>> = groupOpsHelper.groupInfoFlow(groupIds)
    fun joinRequestsFlow(groupId: String): Flow<List<JoinRequest>> = groupMembershipHelper.joinRequestsFlow(groupId)
    suspend fun acceptJoinRequest(request: JoinRequest) = groupMembershipHelper.acceptJoinRequest(request)
    suspend fun declineJoinRequest(request: JoinRequest) = groupMembershipHelper.declineJoinRequest(request)
    fun personalNotificationsFlow() = notificationHelper.personalNotificationsFlow()
    fun groupNotificationsFlow(groupId: String) = notificationHelper.groupNotificationsFlow(groupId)
    suspend fun markNotificationAsRead(notification: UserNotification) = notificationHelper.markNotificationAsRead(notification)
    suspend fun sendEventNotification(groupId: String, eventTitle: String, googleEventId: String) = notificationHelper.sendEventNotification(groupId, eventTitle, googleEventId)
    suspend fun sendEventResponseNotification(groupId: String, eventTitle: String, accepted: Boolean, googleEventId: String) = notificationHelper.sendEventResponseNotification(groupId, eventTitle, accepted, googleEventId)
    suspend fun sendEventDeletedNotification(groupId: String, eventTitle: String) = notificationHelper.sendEventDeletedNotification(groupId, eventTitle)
    suspend fun sendEventCommentNotification(groupId: String, eventTitle: String, googleEventId: String, commentCount: Int) = notificationHelper.sendEventCommentNotification(groupId, eventTitle, googleEventId, commentCount)
    suspend fun markEventNotificationsAsRead(googleEventId: String) = notificationHelper.markEventNotificationsAsRead(googleEventId)

    fun getTrackersFlow(groupId: String) = trackerHelper.getTrackersFlow(groupId)
    fun getTrackerRecordsFlow(groupId: String, trackerId: String) = trackerHelper.getRecordsFlow(groupId, trackerId)
    suspend fun addTracker(groupId: String, tracker: Tracker) = trackerHelper.addTracker(groupId, tracker)
    suspend fun updateTracker(groupId: String, tracker: Tracker) = trackerHelper.updateTracker(groupId, tracker)
    suspend fun deleteTracker(groupId: String, trackerId: String) = trackerHelper.deleteTracker(groupId, trackerId)
    suspend fun addTrackerRecord(groupId: String, record: TrackerRecord) = trackerHelper.addRecord(groupId, record)
    suspend fun updateTrackerRecord(groupId: String, record: TrackerRecord) = trackerHelper.updateRecord(groupId, record)
    suspend fun deleteTrackerRecord(groupId: String, recordId: String) = trackerHelper.deleteRecord(groupId, recordId)

    suspend fun saveCalendarExtension(extension: CalendarEventExtension) = calendarExtensionHelper.saveExtension(extension)
    suspend fun getCalendarExtension(googleEventId: String) = calendarExtensionHelper.getExtension(googleEventId)
    fun calendarExtensionFlow(googleEventId: String) = calendarExtensionHelper.getExtensionFlow(googleEventId)
    suspend fun getCalendarExtensionsForRange(startMs: Long, endMs: Long) = calendarExtensionHelper.getExtensionsForRange(startMs, endMs)
    suspend fun getAllCalendarExtensions() = calendarExtensionHelper.getAllExtensions()
    suspend fun deleteCalendarExtension(googleEventId: String) = calendarExtensionHelper.deleteExtension(googleEventId)
    fun calendarCommentsFlow(googleEventId: String) = calendarExtensionHelper.getCommentsCollection(googleEventId)
    suspend fun addCalendarComment(googleEventId: String, comment: Comment) = calendarExtensionHelper.addComment(googleEventId, comment)
}
