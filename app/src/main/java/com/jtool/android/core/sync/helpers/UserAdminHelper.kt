package com.jtool.android.core.sync.helpers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.GroupInfo
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

    suspend fun pushUserSettings(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val appUserId = settings.getAppUserId()
        val globalUserName = settings.getUserName()
        return try {
            val userDoc = getUsersColl().document(uid)
            val snapshot = userDoc.get().await()
            val isAdmin = snapshot.getBoolean("isAdmin") ?: false
            val localGroupIds = settings.getGroupIds()
            @Suppress("UNCHECKED_CAST")
            val serverGroupIds = (snapshot.get("groupIds") as? List<String>) ?: emptyList()
            val mergedGroupIds = (localGroupIds + serverGroupIds).distinct()
            val data = mutableMapOf(
                "currentGroupId" to (settings.getCurrentGroupId() ?: snapshot.getString("currentGroupId")),
                "groupIds" to mergedGroupIds,
                "appUserId" to appUserId,
                "email" to auth.currentUser?.email,
                "isDarkMode" to settings.isDarkMode(),
                "language" to settings.getLanguage(),
                "isAdmin" to isAdmin
            )
            settings.getAllWeeklyHours().forEach { (day, hours) ->
                data["hours_${day.name}"] = hours
            }
            userDoc.set(data, SetOptions.merge()).await()
            val profile = mapOf("displayName" to globalUserName)
            getPublicProfilesColl().document(appUserId).set(profile, SetOptions.merge()).await()
            getPublicProfilesColl().document(uid).set(profile, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchUserSettings(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val snapshot = getUsersColl().document(uid).get().await()
            if (!snapshot.exists()) return false
            @Suppress("UNCHECKED_CAST")
            val groupIds = snapshot.get("groupIds") as? List<String> ?: emptyList()
            val finalGroupsMap = mutableMapOf<String, GroupInfo>()
            if (groupIds.isNotEmpty()) {
                val gSnapshots = getGroupsColl().whereIn(FieldPath.documentId(), groupIds.take(30)).get().await()
                gSnapshots.documents.forEach { doc ->
                    finalGroupsMap[doc.id] = GroupInfo(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown Group",
                        owner = doc.getString("ownerUid")
                    )
                }
            }
            settings.saveGroups(finalGroupsMap)
            val localCurrentGroupId = settings.getCurrentGroupId()
            if (localCurrentGroupId == null && groupIds.isNotEmpty()) {
                settings.setCurrentGroupId(groupIds.first())
            } else if (localCurrentGroupId != null && !groupIds.contains(localCurrentGroupId)) {
                settings.setCurrentGroupId(groupIds.firstOrNull())
            }
            val pSnap = getPublicProfilesColl().document(uid).get().await()
            pSnap.getString("displayName")?.let { settings.saveUserName(it) }
            snapshot.getString("appUserId")?.let { settings.saveAppUserId(it) }
            DayOfWeek.entries.forEach { day ->
                snapshot.getDouble("hours_${day.name}")?.let { settings.saveDayHours(day, it) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isAdminFlow(): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(false); close(); return@callbackFlow }
        val listener = getUsersColl().document(uid).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.getBoolean("isAdmin") ?: false)
        }
        awaitClose { listener.remove() }
    }

    fun allUsersFlow(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = getUsersColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.plus("uid" to doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) {
        getUsersColl().document(uid).update("isAdmin", isAdmin).await()
    }

    fun allGroupsFlow(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = getGroupsColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.plus("id" to doc.id)
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
            @Suppress("UNCHECKED_CAST")
            val groupIds = userSnap.get("groupIds") as? List<String> ?: emptyList()
            groupIds.forEach { gid ->
                getGroupsColl().document(gid).collection("members").document(uid).delete().await()
            }
            val appUserId = userSnap.getString("appUserId")
            if (appUserId != null) {
                getPublicProfilesColl().document(appUserId).delete().await()
            }
            getPublicProfilesColl().document(uid).delete().await()
            userDoc.delete().await()
        } catch (e: Exception) {}
    }

    suspend fun deleteMyAccount(): Boolean {
        val user = auth.currentUser ?: return false
        val uid = user.uid
        return try {
            val snapshot = getUsersColl().document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            val groupIds = snapshot.get("groupIds") as? List<String> ?: emptyList()
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
}
