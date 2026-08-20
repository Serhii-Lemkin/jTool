package com.jtool.android.core.sync.helpers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.GroupInfo
import com.jtool.android.core.models.User
import com.jtool.android.core.models.UserData
import com.jtool.android.core.settings.SettingsManager
import java.time.DayOfWeek
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserAdminHelper(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsManager,
    private val groupOpsHelperProvider: () -> GroupOperationsHelper
) {
    private fun getUsersColl() = firestore.collection("users")
    private fun getGroupsColl() = firestore.collection("groups")
    private fun getPublicProfilesColl() = firestore.collection("public_profiles")
    private fun getUserDataColl() = firestore.collection("user_data")

    suspend fun pushUserSettings(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val appUserId = settings.getAppUserId()
        val globalUserName = settings.getUserName()
        return try {
            val userDoc = getUsersColl().document(uid)
            val snapshot = userDoc.get().await()
            val userOnServer = snapshot.toObject(User::class.java)
            val isAdmin = userOnServer?.isAdmin ?: false
            
            // Core identity in /users/
            val localGroupIds = settings.getGroupIds()
            val serverGroupIds = userOnServer?.groupIds ?: emptyList()
            val mergedGroupIds = (localGroupIds + serverGroupIds).distinct()
            val coreData = mapOf(
                "email" to auth.currentUser?.email,
                "groupIds" to mergedGroupIds,
                "isAdmin" to isAdmin
            )
            userDoc.set(coreData, SetOptions.merge()).await()
            
            // Stats and settings in /user_data/
            val hoursMap = settings.getAllWeeklyHours().mapKeys { it.key.name }
            val statsData = mapOf(
                "appUserId" to appUserId,
                "currentGroupId" to settings.getCurrentGroupId(),
                "isDarkMode" to settings.isDarkMode(),
                "language" to settings.getLanguage(),
                "hours" to hoursMap
            )
            getUserDataColl().document(uid).set(statsData, SetOptions.merge()).await()
            
            val profile = mapOf("displayName" to globalUserName)
            getPublicProfilesColl().document(uid).set(profile, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchUserSettings(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            // 1. Fetch core user info
            val snapshot = getUsersColl().document(uid).get().await()
            val user = snapshot.toObject(User::class.java) ?: return false
            
            // 2. Fetch stats and settings
            val statsSnap = getUserDataColl().document(uid).get().await()
            val stats = statsSnap.toObject(UserData::class.java) ?: UserData()
            
            val groupIds = user.groupIds
            val finalGroupsMap = mutableMapOf<String, GroupInfo>()
            if (groupIds.isNotEmpty()) {
                val gSnapshots = getGroupsColl().whereIn(FieldPath.documentId(), groupIds.take(30)).get().await()
                gSnapshots.documents.forEach { doc ->
                    val group = doc.toObject(GroupInfo::class.java)
                    finalGroupsMap[doc.id] = GroupInfo(
                        id = doc.id,
                        name = group?.name ?: "Unknown Group",
                        owner = group?.owner
                    )
                }
            }
            settings.saveGroups(finalGroupsMap)
            
            val localCurrentGroupId = settings.getCurrentGroupId()
            val remoteCurrentGroupId = stats.currentGroupId
            if (localCurrentGroupId == null && groupIds.isNotEmpty()) {
                settings.setCurrentGroupId(remoteCurrentGroupId ?: groupIds.first())
            } else if (localCurrentGroupId != null && !groupIds.contains(localCurrentGroupId)) {
                settings.setCurrentGroupId(groupIds.firstOrNull())
            }
            
            val pSnap = getPublicProfilesColl().document(uid).get().await()
            pSnap.getString("displayName")?.let { settings.saveUserName(it) }
            
            if (stats.appUserId.isNotEmpty()) settings.saveAppUserId(stats.appUserId)
            settings.setDarkMode(stats.isDarkMode)
            settings.setLanguage(stats.language)
            
            stats.hours.forEach { (dayName, hours) ->
                try {
                    val day = DayOfWeek.valueOf(dayName)
                    settings.saveDayHours(day, hours)
                } catch (_: Exception) {}
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("UserAdmin", "Fetch settings failed", e)
            false
        }
    }

    fun isAdminFlow(): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(false); close(); return@callbackFlow }
        val listener = getUsersColl().document(uid).addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java)
            trySend(user?.isAdmin ?: false)
        }
        awaitClose { listener.remove() }
    }

    fun allUsersFlow(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = getUsersColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.map { doc ->
                    val user = doc.toObject(User::class.java)
                    val data = mutableMapOf<String, Any>(
                        "uid" to doc.id,
                        "email" to (user?.email ?: ""),
                        "isAdmin" to (user?.isAdmin ?: false)
                    )
                    data
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) {
        getUsersColl().document(uid).set(mapOf("isAdmin" to isAdmin), SetOptions.merge()).await()
    }

    fun allGroupsFlow(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = getGroupsColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val group = doc.toObject(GroupInfo::class.java)
                    group?.let {
                        mapOf(
                            "id" to doc.id,
                            "name" to it.name,
                            "ownerUid" to (it.owner ?: "")
                        )
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun deleteGroupPermanently(groupId: String) {
        try {
            val groupDoc = getGroupsColl().document(groupId)
            val members = groupDoc.collection("members").get().await()
            members.documents.forEach { it.reference.delete() }
            groupDoc.delete().await()
            if (settings.getCurrentGroupId() == groupId) {
                settings.setCurrentGroupId(null)
            }
            settings.removeGroup(groupId)
        } catch (e: Exception) {}
    }

    suspend fun deleteUserPermanently(uid: String) {
        try {
            val userDoc = getUsersColl().document(uid)
            val userSnap = userDoc.get().await()
            val user = userSnap.toObject(User::class.java)
            val groupIds = user?.groupIds ?: emptyList()
            groupIds.forEach { gid ->
                getGroupsColl().document(gid).collection("members").document(uid).delete().await()
            }
            
            // Delete user data and public profile
            getUserDataColl().document(uid).delete().await()
            getPublicProfilesColl().document(uid).delete().await()
            userDoc.delete().await()
        } catch (e: Exception) {
            android.util.Log.e("UserAdmin", "Failed to delete user", e)
        }
    }

    suspend fun deleteMyAccount(): Boolean {
        val user = auth.currentUser ?: return false
        val uid = user.uid
        return try {
            val snapshot = getUsersColl().document(uid).get().await()
            val userObj = snapshot.toObject(User::class.java)
            val groupIds = userObj?.groupIds ?: emptyList()
            groupIds.forEach { gid ->
                groupOpsHelperProvider().leaveGroup(gid)
            }
            val appUserId = settings.getAppUserId()
            getPublicProfilesColl().document(appUserId).delete().await()
            getPublicProfilesColl().document(uid).delete().await()
            getUsersColl().document(uid).delete().await()
            user.delete().await()
            settings.setCurrentGroupId(null)
            settings.clearAllGroups()
            groupOpsHelperProvider().clearLocalCache()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun userNamesFlow(): Flow<Map<String, String>> = callbackFlow {
        val listener = getPublicProfilesColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val map = snapshot.documents.associateBy({ it.id }, { doc ->
                    doc.getString("displayName") ?: "Unknown"
                })
                trySend(map)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun migrateAllUsersToUserData(onProgress: (String) -> Unit): Boolean {
        return try {
            val usersSnapshot = getUsersColl().get().await()
            val total = usersSnapshot.size()
            var count = 0
            
            usersSnapshot.documents.forEach { doc ->
                val uid = doc.id
                val data = doc.data ?: return@forEach
                
                // Extract settings fields from /users/
                val hoursMap = mutableMapOf<String, Double>()
                data.forEach { (key, value) ->
                    if (key.startsWith("hours_")) {
                        val hours = (value as? Number)?.toDouble() ?: 0.0
                        hoursMap[key.removePrefix("hours_")] = hours
                    }
                }
                
                val statsData = mapOf(
                    "appUserId" to (data["appUserId"] as? String ?: ""),
                    "currentGroupId" to (data["currentGroupId"] as? String),
                    "isDarkMode" to (data["isDarkMode"] as? Boolean ?: true),
                    "language" to (data["language"] as? String ?: "en"),
                    "hours" to hoursMap,
                    "lastSupportDate" to (data["lastSupportDate"] as? String ?: ""),
                    "dailySupportCount" to ((data["dailySupportCount"] as? Long)?.toInt() ?: 0)
                )
                
                // Save to /user_data/
                getUserDataColl().document(uid).set(statsData, SetOptions.merge()).await()
                
                // Cleanup /users/ document (Remove moved fields)
                val coreKeys = listOf("email", "groupIds", "isAdmin")
                val cleanupMap = mutableMapOf<String, Any>()
                data.keys.forEach { key ->
                    if (!coreKeys.contains(key)) {
                        cleanupMap[key] = com.google.firebase.firestore.FieldValue.delete()
                    }
                }
                if (cleanupMap.isNotEmpty()) {
                    getUsersColl().document(uid).update(cleanupMap).await()
                }
                
                count++
                onProgress("Migrated $count/$total")
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("UserAdmin", "Migration failed", e)
            false
        }
    }
}
