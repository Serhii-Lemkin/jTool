package com.jtool.android.core.sync.helpers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.user.User
import com.jtool.android.core.models.user.UserData
import com.jtool.android.core.models.calendar.CalendarConfig
import com.jtool.android.core.settings.SettingsManager
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
            
            val coreData = mapOf(
                User::email.name to auth.currentUser?.email,
                User::isAdmin.name to isAdmin
            )
            userDoc.set(coreData, SetOptions.merge()).await()
            
            val statsData = mapOf(
                UserData::appUserId.name to appUserId,
                UserData::currentGroupId.name to settings.getCurrentGroupId(),
                UserData::isDarkMode.name to settings.isDarkMode(),
                UserData::language.name to settings.getLanguage(),
                UserData::worktimeOptions.name to settings.getWorktimeOptions(),
                "calendarConfig" to settings.getCalendarConfig()
            )
            getUserDataColl().document(uid).set(statsData, SetOptions.merge()).await()
            
            val profileDoc = getPublicProfilesColl().document(uid)
            val profileSnapshot = profileDoc.get().await()
            val existingServerName = profileSnapshot.getString(com.jtool.android.core.models.user.PublicProfile::displayName.name)
            
            val profile = if (existingServerName != null && (globalUserName == "User" || globalUserName.isEmpty())) {
                // Keep server name if local is default
                mapOf(com.jtool.android.core.models.user.PublicProfile::displayName.name to existingServerName)
            } else {
                mapOf(com.jtool.android.core.models.user.PublicProfile::displayName.name to globalUserName)
            }
            
            profileDoc.set(profile, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchUserSettings(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val userRef = getUsersColl().document(uid)
            val snapshot = userRef.get().await()
            val user = snapshot.toObject(User::class.java) ?: return false
            
            val statsSnap = getUserDataColl().document(uid).get().await()
            val stats = statsSnap.toObject(UserData::class.java) ?: UserData()
            
            val groupIds = user.groupIds.toMutableList()
            val finalGroupsMap = mutableMapOf<String, GroupInfo>()
            val validIds = mutableListOf<String>()
            var listChanged = false
            
            // 1. Proactive scan: Find any groups user might be in but are missing from list
            // We can check groups where ownerUid == uid as a baseline for recovery
            val ownedGroups = getGroupsColl().whereEqualTo(GroupInfo::ownerUid.name, uid).get().await()
            ownedGroups.documents.forEach { doc ->
                if (!groupIds.contains(doc.id)) {
                    groupIds.add(doc.id)
                    listChanged = true
                }
            }

            if (groupIds.isNotEmpty()) {
                for (gid in groupIds) {
                    try {
                        val gSnap = getGroupsColl().document(gid).get().await()
                        
                        if (gSnap.exists()) {
                            val memberSnap = getGroupsColl().document(gid).collection("members").document(uid).get().await()
                            
                            val group = gSnap.toObject(GroupInfo::class.java)
                            if (memberSnap.exists() || group?.ownerUid == uid) {
                                if (group != null) {
                                    finalGroupsMap[gid] = group.copy(id = gid)
                                    validIds.add(gid)
                                    
                                    if (!memberSnap.exists() && group.ownerUid == uid) {
                                        getGroupsColl().document(gid).collection("members").document(uid)
                                            .set(com.jtool.android.core.models.group.MemberInfo(joinedAt = System.currentTimeMillis())).await()
                                    }
                                } else {
                                    validIds.add(gid)
                                }
                            } else {
                                listChanged = true
                            }
                        } else {
                            listChanged = true
                        }
                    } catch (e: Exception) {
                        validIds.add(gid)
                        android.util.Log.w("UserAdmin", "Could not verify group $gid: ${e.message}")
                    }
                }
            }
            
            if (listChanged || validIds.size != user.groupIds.size) {
                userRef.update(User::groupIds.name, validIds).await()
            }
            
            settings.saveGroups(finalGroupsMap)
            
            val localCurrentGroupId = settings.getCurrentGroupId()
            val remoteCurrentGroupId = stats.currentGroupId
            
            val targetGroupId = if (localCurrentGroupId != null && validIds.contains(localCurrentGroupId)) {
                localCurrentGroupId
            } else if (remoteCurrentGroupId != null && validIds.contains(remoteCurrentGroupId)) {
                remoteCurrentGroupId
            } else {
                validIds.firstOrNull()
            }
            
            if (targetGroupId != localCurrentGroupId) {
                settings.setCurrentGroupId(targetGroupId)
            }
            
            if (remoteCurrentGroupId != targetGroupId) {
                getUserDataColl().document(uid).update(UserData::currentGroupId.name, targetGroupId).await()
            }
            
            val pSnap = getPublicProfilesColl().document(uid).get().await()
            pSnap.getString(com.jtool.android.core.models.user.PublicProfile::displayName.name)?.let { settings.saveUserName(it) }
            
            if (stats.appUserId.isNotEmpty()) settings.saveAppUserId(stats.appUserId)
            settings.setDarkMode(stats.isDarkMode)
            settings.setLanguage(stats.language)
            settings.saveWorktimeOptions(stats.worktimeOptions)
            settings.saveCalendarConfig(stats.calendarConfig)
            
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

    fun allUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = getUsersColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(User::class.java)?.copy(uid = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("UserAdmin", "Corrupted user document ${doc.id}", e)
                        null
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) {
        getUsersColl().document(uid).set(mapOf(User::isAdmin.name to isAdmin), SetOptions.merge()).await()
    }

    suspend fun clearWorkdayData(uid: String) {
        getUserDataColl().document(uid).update(UserData::worktimeOptions.name, com.google.firebase.firestore.FieldValue.delete()).await()
    }

    suspend fun cleanupLegacyWorkHours() {
        try {
            val snapshots = getUserDataColl().get().await()
            firestore.runBatch { batch ->
                snapshots.documents.forEach { doc ->
                    batch.update(doc.reference, "hours", com.google.firebase.firestore.FieldValue.delete())
                }
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("UserAdmin", "Cleanup failed", e)
        }
    }

    fun allGroupsFlow(): Flow<List<GroupInfo>> = callbackFlow {
        val listener = getGroupsColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(GroupInfo::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("UserAdmin", "Corrupted group document ${doc.id}", e)
                        null
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
            
            val arrayRemove = com.google.firebase.firestore.FieldValue.arrayRemove(groupId)
            members.documents.forEach { memberDoc ->
                val uid = memberDoc.id
                getUsersColl().document(uid).update(User::groupIds.name, arrayRemove).await()
            }
            
            // Delete all member documents
            members.documents.forEach { it.reference.delete().await() }
            
            groupDoc.delete().await()
            
            if (settings.getCurrentGroupId() == groupId) {
                settings.setCurrentGroupId(null)
            }
            settings.removeGroup(groupId)
        } catch (e: Exception) {
            android.util.Log.e("UserAdmin", "Delete group failed", e)
        }
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
            getUserDataColl().document(uid).delete().await() // Delete user_data
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
                    doc.getString(com.jtool.android.core.models.user.PublicProfile::displayName.name) ?: "Unknown"
                })
                trySend(map)
            }
        }
        awaitClose { listener.remove() }
    }
}
