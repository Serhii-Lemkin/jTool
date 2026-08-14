package com.jtool.android.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.models.GroupInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.helpers.GroupOperationsHelper
import com.jtool.android.core.sync.helpers.RTDBMigrationHelper
import com.jtool.android.core.sync.helpers.UserAdminHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class GroupSyncManager(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val database = com.google.firebase.database.FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val settings = SettingsManager(context)

    private val migrationHelper = RTDBMigrationHelper(firestore, database, auth)
    private val userAdminHelper: UserAdminHelper = UserAdminHelper(firestore, auth, settings) { groupOpsHelper }
    private val groupOpsHelper: GroupOperationsHelper = GroupOperationsHelper(context, firestore, auth, settings) { userAdminHelper }

    private fun getAppInfoColl() = firestore.collection("app_info")
    private fun getSuggestionsColl() = firestore.collection("suggestions")

    // --- App Update Logic ---

    suspend fun getLatestAppInfo(): Map<String, Any>? {
        return try {
            val snapshot = getAppInfoColl().document("latest").get().await()
            if (snapshot.exists()) {
                snapshot.data
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun pushSuggestion(text: String, userId: String) {
        try {
            val sanitizedText = text.trim()
            if (sanitizedText.isBlank()) return
            val timestamp = System.currentTimeMillis()
            val suggestionData = mapOf(
                "text" to sanitizedText,
                "userId" to userId,
                "timestamp" to timestamp
            )
            getSuggestionsColl().add(suggestionData).await()
        } catch (e: Exception) {}
    }

    fun suggestionsFlow(): Flow<List<String>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = getSuggestionsColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.getString("text")
                }.distinct()
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun allSuggestionsFlow(): Flow<List<Map<String, String>>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = getSuggestionsColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val map = mutableMapOf<String, String>()
                    map["id"] = doc.id
                    doc.data?.forEach { (key, value) ->
                        map[key] = value?.toString() ?: ""
                    }
                    map
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun deleteSuggestion(id: String) {
        getSuggestionsColl().document(id).delete().await()
    }

    // --- Delegated Methods ---

    fun currentGroupIdFlow(): Flow<String?> = groupOpsHelper.currentGroupIdFlow()
    suspend fun getUserGroups(): List<String> = groupOpsHelper.getUserGroups()
    suspend fun createGroup(groupName: String): String? = groupOpsHelper.createGroup(groupName)
    suspend fun updateGroupName(groupId: String, newName: String): Boolean = groupOpsHelper.updateGroupName(groupId, newName)
    suspend fun joinGroup(groupId: String): Boolean = groupOpsHelper.joinGroup(groupId)
    suspend fun leaveGroup(groupId: String) = groupOpsHelper.leaveGroup(groupId)
    suspend fun switchGroup(groupId: String) = groupOpsHelper.switchGroup(groupId)
    fun clearLocalCache() = groupOpsHelper.clearLocalCache()
    suspend fun pushGroupData(): Boolean = true
    suspend fun manualSyncMerge(): Boolean = true
    suspend fun pullGroupData(): Boolean = true
    suspend fun sendInvite(email: String, groupId: String): Boolean = groupOpsHelper.sendInvite(email, groupId)
    fun invitesFlow(): Flow<List<Map<String, String>>> = groupOpsHelper.invitesFlow()
    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean): Boolean = groupOpsHelper.respondToInvite(inviteId, groupId, accept)
    suspend fun pushUserSettings(): Boolean = userAdminHelper.pushUserSettings()
    suspend fun fetchUserSettings(): Boolean = userAdminHelper.fetchUserSettings()
    fun isAdminFlow(): Flow<Boolean> = userAdminHelper.isAdminFlow()
    fun allUsersFlow(): Flow<List<Map<String, Any>>> = userAdminHelper.allUsersFlow()
    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) = userAdminHelper.setUserAdminStatus(uid, isAdmin)
    fun allGroupsFlow(): Flow<List<Map<String, Any>>> = userAdminHelper.allGroupsFlow()
    suspend fun deleteGroupPermanently(groupId: String) = userAdminHelper.deleteGroupPermanently(groupId)
    suspend fun deleteUserPermanently(uid: String) = userAdminHelper.deleteUserPermanently(uid)
    suspend fun deleteMyAccount(): Boolean = userAdminHelper.deleteMyAccount()
    fun userNamesFlow(): Flow<Map<String, String>> = userAdminHelper.userNamesFlow()
    fun groupMembersFlow(groupId: String): Flow<List<String>> = groupOpsHelper.groupMembersFlow(groupId)
    fun userGroupsFlow(): Flow<List<String>> = groupOpsHelper.userGroupsFlow()
    fun groupInfoFlow(groupIds: List<String>): Flow<Map<String, GroupInfo>> = groupOpsHelper.groupInfoFlow(groupIds)
    suspend fun migrateFromRTDB(onStatusUpdate: (String) -> Unit): Boolean = migrationHelper.migrateFromRTDB(onStatusUpdate)
}
