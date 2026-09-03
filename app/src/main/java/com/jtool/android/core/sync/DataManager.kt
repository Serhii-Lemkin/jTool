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
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow


class DataManager(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val settings = SettingsManager(context)

    private val userAdminHelper: UserAdminHelper = UserAdminHelper(firestore, auth, settings) { groupOpsHelper }
    private val groupOpsHelper: GroupOperationsHelper = GroupOperationsHelper(context, firestore, auth, settings) { userAdminHelper }
    private val supportHelper: SupportDatabaseHelper = SupportDatabaseHelper(firestore)
    private val systemHelper: SystemDatabaseHelper = SystemDatabaseHelper(firestore)
    private val workdayHelper: com.jtool.android.core.sync.helpers.WorkdayDatabaseHelper = com.jtool.android.core.sync.helpers.WorkdayDatabaseHelper(firestore)
    private val calendarHelper: com.jtool.android.core.sync.helpers.CalendarDatabaseHelper = com.jtool.android.core.sync.helpers.CalendarDatabaseHelper(firestore)

    private fun getSuggestionsColl() = firestore.collection("suggestions")

    fun getPrivateCalendarEventsFlow(userId: String, fromDate: String, toDate: String) = calendarHelper.getPrivateEventsFlow(userId, fromDate, toDate)
    fun getSharedCalendarEventsFlow(groupId: String, fromDate: String, toDate: String) = calendarHelper.getSharedEventsFlow(groupId, fromDate, toDate)
    suspend fun addPrivateCalendarEvent(userId: String, event: com.jtool.android.core.models.calendar.CalendarEvent) = calendarHelper.addPrivateEvent(userId, event)
    suspend fun addSharedCalendarEvent(groupId: String, event: com.jtool.android.core.models.calendar.CalendarEvent) = calendarHelper.addSharedEvent(groupId, event)
    suspend fun deletePrivateCalendarEvent(userId: String, eventId: String) = calendarHelper.deletePrivateEvent(userId, eventId)
    suspend fun deleteSharedCalendarEvent(groupId: String, eventId: String) = calendarHelper.deleteSharedEvent(groupId, eventId)
    suspend fun clearPrivateCalendarShifts(userId: String, fromDate: String, toDate: String) = calendarHelper.clearPrivateShiftsInRange(userId, fromDate, toDate)
    suspend fun replacePrivateCalendarShifts(userId: String, fromDate: String, toDate: String, newShifts: List<com.jtool.android.core.models.calendar.CalendarEvent>) = calendarHelper.replacePrivateShiftsInRange(userId, fromDate, toDate, newShifts)

    fun getSupportColl() = firestore.collection("support_requests")
    suspend fun getAppOptions() = supportHelper.getAppOptions()
    suspend fun submitSupportRequest(request: SupportRequest, uid: String, bypassLimit: Boolean = false) = supportHelper.submitRequest(request, uid, bypassLimit)
    fun supportRequestsFlow(isAdmin: Boolean, userId: String) = supportHelper.getRequestsFlow(isAdmin, userId)
    suspend fun updateSupportStatus(id: String, status: SupportStatusEnum) = supportHelper.updateStatus(id, status)
    suspend fun updateSupportType(id: String, type: SupportTypeEnum) = supportHelper.updateType(id, type)
    fun supportCommentsFlow(id: String) = supportHelper.getCommentsFlow(id)
    suspend fun addSupportComment(id: String, comment: com.jtool.android.core.models.common.Comment) = supportHelper.addComment(id, comment)
    suspend fun editSupportComment(id: String, commentId: String, newContent: String) = supportHelper.editComment(id, commentId, newContent)
    suspend fun deleteSupportComment(id: String, commentId: String) = supportHelper.deleteComment(id, commentId)
    suspend fun deleteSupportRequest(id: String) = supportHelper.deleteRequest(id)
    suspend fun logRequestedLanguage(name: String, code: String) = systemHelper.logRequestedLanguage(name, code)

    fun suggestionsFlow(): kotlinx.coroutines.flow.Flow<List<String>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = getSuggestionsColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.getString("text") }.distinct()
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun currentGroupIdFlow(): Flow<String?> = groupOpsHelper.currentGroupIdFlow()
    suspend fun getUserGroups(): List<String> = groupOpsHelper.getUserGroups()
    suspend fun createGroup(groupName: String): String? = groupOpsHelper.createGroup(groupName)
    suspend fun updateGroupName(groupId: String, newName: String): Boolean = groupOpsHelper.updateGroupName(groupId, newName)
    suspend fun joinGroup(groupId: String, isInviteAcceptance: Boolean = false): com.jtool.android.core.models.group.enums.JoinGroupResultEnum = groupOpsHelper.joinGroup(groupId, isInviteAcceptance)
    suspend fun leaveGroup(groupId: String) = groupOpsHelper.leaveGroup(groupId)
    suspend fun switchGroup(groupId: String) = groupOpsHelper.switchGroup(groupId)
    fun clearLocalCache() = groupOpsHelper.clearLocalCache()
    suspend fun pushGroupData(): Boolean = true
    suspend fun manualSyncMerge(): Boolean = true
    suspend fun pullGroupData(): Boolean = true
    suspend fun sendInvite(email: String, groupId: String): Boolean = groupOpsHelper.sendInvite(email, groupId)
    fun invitesFlow(): Flow<List<com.jtool.android.core.models.group.Invite>> = groupOpsHelper.invitesFlow()
    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean): Boolean = groupOpsHelper.respondToInvite(inviteId, groupId, accept)
    suspend fun pushUserSettings(): Boolean = userAdminHelper.pushUserSettings()
    suspend fun fetchUserSettings(): Boolean = userAdminHelper.fetchUserSettings()
    fun isAdminFlow(): Flow<Boolean> = userAdminHelper.isAdminFlow()
    fun allUsersFlow(): Flow<List<com.jtool.android.core.models.user.User>> = userAdminHelper.allUsersFlow()
    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) = userAdminHelper.setUserAdminStatus(uid, isAdmin)
    suspend fun clearWorkdayOptions(uid: String) = userAdminHelper.clearWorkdayData(uid)
    suspend fun cleanupLegacyWorkHours() = userAdminHelper.cleanupLegacyWorkHours()
    fun allGroupsFlow(): Flow<List<com.jtool.android.core.models.group.GroupInfo>> = userAdminHelper.allGroupsFlow()
    suspend fun deleteGroupPermanently(groupId: String) = userAdminHelper.deleteGroupPermanently(groupId)
    suspend fun deleteUserPermanently(uid: String) = userAdminHelper.deleteUserPermanently(uid)
    suspend fun deleteMyAccount(): Boolean = userAdminHelper.deleteMyAccount()
    fun userNamesFlow(): Flow<Map<String, String>> = userAdminHelper.userNamesFlow()
    fun groupMembersFlow(groupId: String): Flow<List<String>> = groupOpsHelper.groupMembersFlow(groupId)
    fun userGroupsFlow(): Flow<List<String>> = groupOpsHelper.userGroupsFlow()
    fun groupInfoFlow(groupIds: List<String>): Flow<Map<String, GroupInfo>> = groupOpsHelper.groupInfoFlow(groupIds)
    fun joinRequestsFlow(groupId: String): Flow<List<com.jtool.android.core.models.group.JoinRequest>> = groupOpsHelper.joinRequestsFlow(groupId)
    suspend fun acceptJoinRequest(request: com.jtool.android.core.models.group.JoinRequest) = groupOpsHelper.acceptJoinRequest(request)
    suspend fun declineJoinRequest(request: com.jtool.android.core.models.group.JoinRequest) = groupOpsHelper.declineJoinRequest(request)
    fun personalNotificationsFlow() = groupOpsHelper.personalNotificationsFlow()
    fun groupNotificationsFlow(groupId: String) = groupOpsHelper.groupNotificationsFlow(groupId)
    suspend fun markNotificationAsRead(notification: com.jtool.android.core.models.common.UserNotification) = groupOpsHelper.markNotificationAsRead(notification)
}
