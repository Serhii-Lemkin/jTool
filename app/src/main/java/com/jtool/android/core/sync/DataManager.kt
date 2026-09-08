package com.jtool.android.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.helpers.GroupOperationsHelper
import com.jtool.android.core.sync.helpers.SupportDatabaseHelper
import com.jtool.android.core.sync.helpers.UserAdminHelper
import com.jtool.android.core.sync.helpers.SystemDatabaseHelper
import com.jtool.android.core.sync.helpers.WorkdayDatabaseHelper
import com.jtool.android.core.sync.helpers.CalendarDatabaseHelper
import com.jtool.android.core.sync.helpers.TrackerDatabaseHelper
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import com.jtool.android.core.models.calendar.CalendarEvent
import com.jtool.android.core.models.common.Comment
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.group.Invite
import com.jtool.android.core.models.group.JoinRequest
import com.jtool.android.core.models.tracker.Tracker
import com.jtool.android.core.models.tracker.TrackerRecord
import com.jtool.android.core.models.user.User
import com.jtool.android.core.models.workday.WorkHistoryItem
import com.jtool.android.features.calendar.common.GoogleCalendarSyncHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class DataManager(val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val settings = SettingsManager(context)

    private val userAdminHelper: UserAdminHelper = UserAdminHelper(firestore, auth, settings) { groupOpsHelper }
    private val groupOpsHelper: GroupOperationsHelper = GroupOperationsHelper(context, firestore, auth, settings) { userAdminHelper }
    private val supportHelper: SupportDatabaseHelper = SupportDatabaseHelper(firestore)
    private val systemHelper: SystemDatabaseHelper = SystemDatabaseHelper(firestore)
    private val workdayHelper: WorkdayDatabaseHelper = WorkdayDatabaseHelper(firestore)
    private val calendarHelper: CalendarDatabaseHelper = CalendarDatabaseHelper(firestore)
    private val trackerHelper: TrackerDatabaseHelper = TrackerDatabaseHelper(firestore)

    // --- Unified Calendar Methods ---

    fun allCalendarEventsFlow(fromMs: Long, toMs: Long): Flow<List<CalendarEvent>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        val groupIds = settings.getGroups().keys.toList()
        return calendarHelper.getAllEventsFlow(uid, groupIds, fromMs, toMs)
    }

    suspend fun allCalendarEventsOneTime(fromMs: Long, toMs: Long): List<CalendarEvent> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val groupIds = settings.getGroups().keys.toList()
        return calendarHelper.getAllEventsOneTime(uid, groupIds, fromMs, toMs)
    }

    suspend fun saveCalendarEvent(event: CalendarEvent) {
        val uid = auth.currentUser?.uid ?: return
        val finalEvent = if (event.ownerUid.isEmpty()) event.copy(ownerUid = uid) else event
        calendarHelper.saveEvent(finalEvent)
    }

    suspend fun deleteCalendarEvent(eventId: String) {
        val uid = auth.currentUser?.uid ?: return
        
        val doc = firestore.collection("calendar_events").document(eventId).get().await()
        if (!doc.exists()) return
        
        val event = doc.toObject(CalendarEvent::class.java) ?: return

        if (event.ownerUid != uid) return

        if (!event.googleEventId.isNullOrEmpty()) {
            try {
                GoogleCalendarSyncHelper.deleteFromGoogleCalendar(context, event.googleEventId)
            } catch (_: Exception) {}
        }

        calendarHelper.deleteEvent(eventId)
    }

    suspend fun deleteCalendarEventLocalOnly(eventId: String) {
        val uid = auth.currentUser?.uid ?: return
        
        val doc = firestore.collection("calendar_events").document(eventId).get().await()
        if (!doc.exists()) return
        
        val event = doc.toObject(CalendarEvent::class.java) ?: return
        if (event.ownerUid != uid) return

        calendarHelper.deleteEventLocalOnly(eventId)
    }

    suspend fun clearCalendarShifts(userId: String, groupIds: List<String>, fromMs: Long, toMs: Long) {
        calendarHelper.clearShiftsInRange(userId, groupIds, fromMs, toMs)
    }

    // --- Legacy / Compatibility ---

    fun getPrivateCalendarEventsFlow(userId: String, fromDate: String, toDate: String) = calendarHelper.getPrivateEventsFlow(userId, fromDate, toDate)
    fun getCalendarEventsFlow(groupId: String, fromDate: String, toDate: String) = calendarHelper.getSharedEventsFlow(groupId, fromDate, toDate)
    
    // --- Comments (Centralized) ---

    fun calendarCommentsFlow(eventId: String) = calendarHelper.getEventCommentsFlow(eventId)
    suspend fun addCalendarComment(eventId: String, comment: Comment) = calendarHelper.addComment(eventId, comment)
    suspend fun updateCalendarComment(eventId: String, commentId: String, newContent: String) = calendarHelper.updateComment(eventId, commentId, newContent)
    suspend fun deleteCalendarComment(eventId: String, commentId: String) = calendarHelper.deleteComment(eventId, commentId)

    // --- Support & System ---

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

    // --- Groups ---

    fun currentGroupIdFlow(): Flow<String?> = groupOpsHelper.currentGroupIdFlow()
    suspend fun getUserGroups(): List<String> = groupOpsHelper.getUserGroups()
    suspend fun createGroup(groupName: String, isPrivate: Boolean = false): String? = groupOpsHelper.createGroup(groupName, isPrivate)
    suspend fun ensurePrivateGroupExists(): Boolean = groupOpsHelper.ensurePrivateGroupExists()
    suspend fun updateGroupName(groupId: String, newName: String): Boolean = groupOpsHelper.updateGroupName(groupId, newName)
    suspend fun joinGroup(groupId: String, isInviteAcceptance: Boolean = false) = groupOpsHelper.joinGroup(groupId, isInviteAcceptance)
    suspend fun leaveGroup(groupId: String) = groupOpsHelper.leaveGroup(groupId)
    suspend fun switchGroup(groupId: String) = groupOpsHelper.switchGroup(groupId)
    fun clearLocalCache() = groupOpsHelper.clearLocalCache()
    suspend fun sendInvite(email: String, groupId: String): Boolean = groupOpsHelper.sendInvite(email, groupId)
    fun invitesFlow(): Flow<List<Invite>> = groupOpsHelper.invitesFlow()
    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean): Boolean = groupOpsHelper.respondToInvite(inviteId, groupId, accept)
    
    // --- User Profile ---

    suspend fun pushUserSettings(): Boolean = userAdminHelper.pushUserSettings()
    suspend fun fetchUserSettings(): Boolean = userAdminHelper.fetchUserSettings()
    fun isAdminFlow(): Flow<Boolean> = userAdminHelper.isAdminFlow()
    fun allUsersFlow(): Flow<List<User>> = userAdminHelper.allUsersFlow()
    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) = userAdminHelper.setUserAdminStatus(uid, isAdmin)
    suspend fun clearWorkdayOptions(uid: String) = userAdminHelper.clearWorkdayData(uid)
    suspend fun cleanupLegacyWorkHours() = userAdminHelper.cleanupLegacyWorkHours()
    fun allGroupsFlow(): Flow<List<GroupInfo>> = userAdminHelper.allGroupsFlow()
    suspend fun deleteGroupPermanently(groupId: String) = userAdminHelper.deleteGroupPermanently(groupId)
    suspend fun deleteUserPermanently(uid: String) = userAdminHelper.deleteUserPermanently(uid)
    suspend fun deleteMyAccount(): Boolean = userAdminHelper.deleteMyAccount()
    fun userNamesFlow(): Flow<Map<String, String>> = userAdminHelper.userNamesFlow()
    fun groupMembersFlow(groupId: String): Flow<List<String>> = groupOpsHelper.groupMembersFlow(groupId)
    fun userGroupsFlow(): Flow<List<String>> = groupOpsHelper.userGroupsFlow()
    fun groupInfoFlow(groupIds: List<String>): Flow<Map<String, GroupInfo>> = groupOpsHelper.groupInfoFlow(groupIds)
    fun joinRequestsFlow(groupId: String): Flow<List<JoinRequest>> = groupOpsHelper.joinRequestsFlow(groupId)
    suspend fun acceptJoinRequest(request: JoinRequest) = groupOpsHelper.acceptJoinRequest(request)
    suspend fun declineJoinRequest(request: JoinRequest) = groupOpsHelper.declineJoinRequest(request)
    fun personalNotificationsFlow() = groupOpsHelper.personalNotificationsFlow()
    fun groupNotificationsFlow(groupId: String) = groupOpsHelper.groupNotificationsFlow(groupId)
    suspend fun markNotificationAsRead(notification: UserNotification) = groupOpsHelper.markNotificationAsRead(notification)

    // --- Trackers ---

    fun getTrackersFlow(groupId: String) = trackerHelper.getTrackersFlow(groupId)
    fun getTrackerRecordsFlow(groupId: String, trackerId: String) = trackerHelper.getRecordsFlow(groupId, trackerId)
    suspend fun addTracker(groupId: String, tracker: Tracker) = trackerHelper.addTracker(groupId, tracker)
    suspend fun updateTracker(groupId: String, tracker: Tracker) = trackerHelper.updateTracker(groupId, tracker)
    suspend fun deleteTracker(groupId: String, trackerId: String) = trackerHelper.deleteTracker(groupId, trackerId)
    suspend fun addTrackerRecord(groupId: String, record: TrackerRecord) = trackerHelper.addRecord(groupId, record)
    suspend fun updateTrackerRecord(groupId: String, record: TrackerRecord) = trackerHelper.updateRecord(groupId, record)
    suspend fun deleteTrackerRecord(groupId: String, recordId: String) = trackerHelper.deleteRecord(groupId, recordId)
}
