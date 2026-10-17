package com.jtool.android.core.sync.helpers

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.group.MemberInfo
import com.jtool.android.core.models.user.User
import com.jtool.android.core.models.user.UserData
import com.jtool.android.core.models.user.PublicProfile
import com.jtool.android.core.settings.SettingsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserAdminHelper(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsManager,
    private val groupOpsHelperProvider: () -> GroupOperationsHelper
) {
    private fun getUsersColl() = firestore.collection("users")
    private fun getGroupsColl() = firestore.collection("groups")
    private fun getPublicProfilesColl() = firestore.collection("public_profiles")
    fun getUserDataColl() = firestore.collection("user_data")

    suspend fun pushUserSettings(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val appUserId = settings.getAppUserId()
        val globalUserName = settings.getUserName()
        val now = System.currentTimeMillis()
        return try {
            val userDoc = getUsersColl().document(uid)
            val snapshot = userDoc.get().await()
            val userOnServer = snapshot.toObject(User::class.java)
            val isAdmin = userOnServer?.isAdmin ?: false
            val isReviewer = userOnServer?.isReviewer ?: false
            
            val coreData = mapOf(
                User::email.name to auth.currentUser?.email,
                User::isAdmin.name to isAdmin,
                User::isReviewer.name to isReviewer,
                User::lastModified.name to now
            )
            userDoc.set(coreData, SetOptions.merge()).await()
            
            val statsData = mapOf(
                UserData::appUserId.name to appUserId,
                UserData::currentGroupId.name to settings.getCurrentGroupId(),
                UserData::isDarkMode.name to settings.isDarkMode(),
                UserData::language.name to settings.getLanguage(),
                UserData::worktimeOptions.name to settings.getWorktimeOptions(),
                "calendarConfig" to settings.getCalendarConfig(),
                "lightPrimary" to settings.getLightPrimaryColor(),
                "lightBg" to settings.getLightBgColor(),
                "darkPrimary" to settings.getDarkPrimaryColor(),
                "darkBg" to settings.getDarkBgColor(),
                "seenVaultsInfo" to settings.getSeenVaultsInfo(),
                "jtoolsFolderId" to settings.getJtoolsFolderId(),
                "vaultsFolderId" to settings.getVaultsFolderId()
            )
            getUserDataColl().document(uid).set(statsData, SetOptions.merge()).await()
            
            val profileDoc = getPublicProfilesColl().document(uid)
            val profileSnapshot = profileDoc.get().await()
            val existingServerName = profileSnapshot.getString(PublicProfile::displayName.name)
            
            val authEmail = auth.currentUser?.email
            val calEmail = settings.getGoogleCalendarEmail()
            val profile = PublicProfile(
                uid = uid,
                displayName = if (existingServerName != null && (globalUserName == "User" || globalUserName.isEmpty())) existingServerName else globalUserName,
                email = authEmail?.ifEmpty { null },
                calendarEmail = calEmail.ifEmpty { authEmail ?: "" }.ifEmpty { null },
                lastModified = now
            )
            
            profileDoc.set(profile).await()
            
            // Update global metadata to trigger others to refresh
            firestore.collection("metadata").document("public_profiles")
                .set(mapOf("lastUpdated" to now), SetOptions.merge()).await()
            
            settings.setLastSyncTimestamp(now)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateSeenVaultsInfo(uid: String, seen: Boolean) = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(UserData::seenVaultsInfo.name to seen)
            getUserDataColl().document(uid).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("UserAdminHelper", "Failed to update seenVaultsInfo", e)
        }
    }

    suspend fun fetchUserSettings(force: Boolean = false): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            try {
                val pSnap = getPublicProfilesColl().document(uid).get().await()
                val serverName = pSnap.getString(PublicProfile::displayName.name)
                if (!serverName.isNullOrEmpty()) {
                    settings.saveUserName(serverName)
                }
                pSnap.getString(PublicProfile::calendarEmail.name)?.let { settings.setGoogleCalendarEmail(it) }
            } catch (_: Exception) {}

            val userRef = getUsersColl().document(uid)
            val snapshot = userRef.get().await()
            val user = snapshot.toObject(User::class.java) ?: return false
            
            val localLastSync = settings.getLastSyncTimestamp()
            if (!force && user.lastModified != 0L && user.lastModified <= localLastSync) {
                return true
            }

            val statsSnap = getUserDataColl().document(uid).get().await()
            val stats = statsSnap.toObject(UserData::class.java) ?: UserData()
            settings.setSeenVaultsInfo(stats.seenVaultsInfo)
            settings.setJtoolsFolderId(stats.jtoolsFolderId)
            settings.setVaultsFolderId(stats.vaultsFolderId)
            
            val groupIds = user.groupIds.toMutableList()
            val finalGroupsMap = mutableMapOf<String, GroupInfo>()
            val validIds = mutableListOf<String>()
            var listChanged = false
            
            if (force || localLastSync == 0L) {
                val ownedGroups = getGroupsColl().whereEqualTo(GroupInfo::ownerUid.name, uid).get().await()
                ownedGroups.documents.forEach { doc ->
                    if (!groupIds.contains(doc.id)) {
                        groupIds.add(doc.id)
                        listChanged = true
                    }
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
                                            .set(MemberInfo(joinedAt = System.currentTimeMillis())).await()
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
            
            if (targetGroupId != localCurrentGroupId) settings.setCurrentGroupId(targetGroupId)
            if (remoteCurrentGroupId != targetGroupId) {
                getUserDataColl().document(uid).update(UserData::currentGroupId.name, targetGroupId).await()
            }
            
            val pSnap = getPublicProfilesColl().document(uid).get().await()
            pSnap.getString(PublicProfile::displayName.name)?.let { settings.saveUserName(it) }
            pSnap.getString(PublicProfile::calendarEmail.name)?.let { settings.setGoogleCalendarEmail(it) }
            
            if (stats.appUserId.isNotEmpty()) settings.saveAppUserId(stats.appUserId)
            settings.setDarkMode(stats.isDarkMode)
            settings.setLanguage(stats.language)
            settings.saveWorktimeOptions(stats.worktimeOptions)
            settings.saveCalendarConfig(stats.calendarConfig)
            settings.setLightPrimaryColor(stats.lightPrimary)
            settings.setLightBgColor(stats.lightBg)
            settings.setDarkPrimaryColor(stats.darkPrimary)
            settings.setDarkBgColor(stats.darkBg)
            settings.setLastSyncTimestamp(user.lastModified)
            
            true
        } catch (e: Exception) {
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

    fun isReviewerFlow(): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(false); close(); return@callbackFlow }
        val listener = getUsersColl().document(uid).addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java)
            trySend(user?.isReviewer ?: false)
        }
        awaitClose { listener.remove() }
    }

    fun allUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = try {
            getUsersColl().addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserAdmin", "allUsersFlow listen failed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(User::class.java)?.copy(uid = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        } catch (e: Exception) {
            Log.e("UserAdmin", "Failed to setup allUsersFlow listener", e)
            trySend(emptyList())
            null
        }
        awaitClose { listener?.remove() }
    }

    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) {
        getUsersColl().document(uid).set(mapOf(User::isAdmin.name to isAdmin), SetOptions.merge()).await()
    }

    suspend fun setUserReviewerStatus(uid: String, isReviewer: Boolean) {
        getUsersColl().document(uid).set(mapOf(User::isReviewer.name to isReviewer), SetOptions.merge()).await()
    }

    suspend fun clearWorkdayData(uid: String) {
        getUserDataColl().document(uid).update(UserData::worktimeOptions.name, FieldValue.delete()).await()
    }

    suspend fun cleanupLegacyWorkHours() {
        try {
            val snapshots = getUserDataColl().get().await()
            firestore.runBatch { batch ->
                snapshots.documents.forEach { doc ->
                    batch.update(doc.reference, "hours", FieldValue.delete())
                }
            }.await()
        } catch (e: Exception) {
            Log.e("UserAdmin", "Cleanup failed", e)
        }
    }

    fun allGroupsFlow(): Flow<List<GroupInfo>> = callbackFlow {
        val listener = getGroupsColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(GroupInfo::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
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
            val arrayRemove = FieldValue.arrayRemove(groupId)
            members.documents.forEach { memberDoc ->
                getUsersColl().document(memberDoc.id).update(User::groupIds.name, arrayRemove).await()
            }
            members.documents.forEach { it.reference.delete().await() }
            groupDoc.delete().await()
            if (settings.getCurrentGroupId() == groupId) settings.setCurrentGroupId(null)
            settings.removeGroup(groupId)
        } catch (e: Exception) {
            Log.e("UserAdmin", "Delete group failed", e)
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
            Log.e("UserAdmin", "Failed to delete user", e)
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
            getUserDataColl().document(uid).delete().await()
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
        val listener = try {
            getPublicProfilesColl().addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserAdmin", "userNamesFlow listen failed: ${error.message}")
                    trySend(emptyMap())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val map = snapshot.documents.associateBy({ it.id }, { doc ->
                        doc.getString(PublicProfile::displayName.name) ?: "Unknown"
                    })
                    trySend(map)
                }
            }
        } catch (e: Exception) {
            Log.e("UserAdmin", "Failed to setup userNamesFlow listener", e)
            trySend(emptyMap())
            null
        }
        awaitClose { listener?.remove() }
    }

    fun publicProfilesFlow(): Flow<Map<String, PublicProfile>> = callbackFlow {
        val cached = settings.getPublicProfiles()
        if (cached.isNotEmpty()) trySend(cached)

        val listener = try {
            getPublicProfilesColl().addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserAdmin", "Profiles listen failed: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val freshProfiles = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PublicProfile::class.java)?.copy(uid = doc.id)
                    }
                    val map = freshProfiles.associateBy { it.uid }
                    settings.savePublicProfiles(map)
                    trySend(map)
                }
            }
        } catch (e: Exception) {
            Log.e("UserAdmin", "Failed to setup publicProfilesFlow listener", e)
            null
        }
        awaitClose { listener?.remove() }
    }
}
