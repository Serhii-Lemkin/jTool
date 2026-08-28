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
    private fun getSupportColl() = firestore.collection("support_requests")

    suspend fun pushUserSettings(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val appUserId = settings.getAppUserId()
        val globalUserName = settings.getUserName()
        return try {
            val userDoc = getUsersColl().document(uid)
            val snapshot = userDoc.get().await()
            val userOnServer = snapshot.toObject(User::class.java)
            val isAdmin = userOnServer?.isAdmin ?: false
            
            // Core identity in /users/ (groupIds are managed by dedicated join/leave functions)
            val coreData = mapOf(
                User::email.name to auth.currentUser?.email,
                User::isAdmin.name to isAdmin
            )
            userDoc.set(coreData, SetOptions.merge()).await()
            
            // Stats and settings in /user_data/
            val hoursMap = settings.getAllWeeklyHours().mapKeys { it.key.name }
            val statsData = mapOf(
                UserData::appUserId.name to appUserId,
                UserData::currentGroupId.name to settings.getCurrentGroupId(),
                UserData::isDarkMode.name to settings.isDarkMode(),
                UserData::language.name to settings.getLanguage(),
                UserData::hours.name to hoursMap
            )
            getUserDataColl().document(uid).set(statsData, SetOptions.merge()).await()
            
            val profile = mapOf(com.jtool.android.core.models.PublicProfile::displayName.name to globalUserName)
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
                        ownerUid = group?.ownerUid
                    )
                }
            }
            settings.saveGroups(finalGroupsMap)
            
            val localCurrentGroupId = settings.getCurrentGroupId()
            val remoteCurrentGroupId = stats.currentGroupId
            
            if (localCurrentGroupId != null && remoteCurrentGroupId == null) {
                // Phone knows more than server - sync up
                pushUserSettings()
            } else if (localCurrentGroupId == null && groupIds.isNotEmpty()) {
                settings.setCurrentGroupId(remoteCurrentGroupId ?: groupIds.first())
            } else if (localCurrentGroupId != null && !groupIds.contains(localCurrentGroupId)) {
                settings.setCurrentGroupId(groupIds.firstOrNull())
            }
            
            val pSnap = getPublicProfilesColl().document(uid).get().await()
            pSnap.getString(com.jtool.android.core.models.PublicProfile::displayName.name)?.let { settings.saveUserName(it) }
            
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
                        User::uid.name to doc.id,
                        User::email.name to (user?.email ?: ""),
                        User::isAdmin.name to (user?.isAdmin ?: false)
                    )
                    data
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun setUserAdminStatus(uid: String, isAdmin: Boolean) {
        getUsersColl().document(uid).set(mapOf(User::isAdmin.name to isAdmin), SetOptions.merge()).await()
    }

    fun allGroupsFlow(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = getGroupsColl().addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val group = doc.toObject(GroupInfo::class.java)
                    group?.let {
                        mapOf(
                            GroupInfo::id.name to doc.id,
                            GroupInfo::name.name to it.name,
                            GroupInfo::ownerUid.name to (it.ownerUid ?: "")
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
            
            val arrayRemove = com.google.firebase.firestore.FieldValue.arrayRemove(groupId)
            members.documents.forEach { memberDoc ->
                val uid = memberDoc.id
                getUsersColl().document(uid).update("groupIds", arrayRemove).await()
            }
            
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
                    doc.getString(com.jtool.android.core.models.PublicProfile::displayName.name) ?: "Unknown"
                })
                trySend(map)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun migrateLegacyFields(onProgress: (String) -> Unit): Boolean {
        return try {
            var count = 0
            
            // 1. Users

            val usersSnapshot = getUsersColl().get().await()
            val totalUsersRoot = usersSnapshot.size()
            usersSnapshot.documents.forEachIndexed { index, doc ->
                val data = doc.data ?: return@forEachIndexed
                val updates = mutableMapOf<String, Any>()
                if (data.containsKey("admin")) {
                    updates["isAdmin"] = data["admin"]!!
                    updates["admin"] = com.google.firebase.firestore.FieldValue.delete()
                }
                if (updates.isNotEmpty()) {
                    doc.reference.update(updates).await()
                    count++
                }
                onProgress("Users: ${index + 1}/$totalUsersRoot (Updates: $count)")
            }

            // 2. Support Requests Comments
            val supportSnapshot = getSupportColl().get().await()
            val totalSupport = supportSnapshot.size()
            supportSnapshot.documents.forEachIndexed { index, doc ->
                val commentsColl = doc.reference.collection("comments")
                val commentsSnapshot = commentsColl.get().await()
                commentsSnapshot.documents.forEach { cDoc ->
                    val data = cDoc.data ?: return@forEach
                    val updates = mutableMapOf<String, Any>()
                    if (data.containsKey("edited")) {
                        updates["isEdited"] = data["edited"]!!
                        updates["edited"] = com.google.firebase.firestore.FieldValue.delete()
                    }
                    if (data.containsKey("pending")) {
                        updates["isPending"] = data["pending"]!!
                        updates["pending"] = com.google.firebase.firestore.FieldValue.delete()
                    }
                    if (updates.isNotEmpty()) {
                        cDoc.reference.update(updates).await()
                        count++
                    }
                }
                onProgress("Support Comments: ${index + 1}/$totalSupport (Updates: $count)")
            }

            // 2. User Data
            val userDataSnapshot = getUserDataColl().get().await()
            val totalUsers = userDataSnapshot.size()
            userDataSnapshot.documents.forEachIndexed { index, doc ->
                val data = doc.data ?: return@forEachIndexed
                val updates = mutableMapOf<String, Any>()
                if (data.containsKey("darkMode")) {
                    updates["isDarkMode"] = data["darkMode"]!!
                    updates["darkMode"] = com.google.firebase.firestore.FieldValue.delete()
                }
                if (updates.isNotEmpty()) {
                    doc.reference.update(updates).await()
                    count++
                }
                onProgress("User Data: ${index + 1}/$totalUsers (Updates: $count)")
            }

            // 3. Groups (Notes, Cleaning, Shopping, and Group Metadata)
            val groupsSnapshot = getGroupsColl().get().await()
            val totalGroups = groupsSnapshot.size()
            groupsSnapshot.documents.forEachIndexed { gIndex, gDoc ->
                // Group Metadata Migration (owner -> ownerUid)
                val groupData = gDoc.data ?: emptyMap()
                val gUpdates = mutableMapOf<String, Any>()
                if (groupData.containsKey("owner")) {
                    gUpdates["ownerUid"] = groupData["owner"]!!
                    gUpdates["owner"] = com.google.firebase.firestore.FieldValue.delete()
                }
                if (gUpdates.isNotEmpty()) {
                    gDoc.reference.update(gUpdates).await()
                }

                // Notes (Comments and Checklist)
                val notesColl = gDoc.reference.collection("notes")
                val notesSnapshot = notesColl.get().await()
                notesSnapshot.documents.forEach { nDoc ->
                    // Comments
                    val commentsColl = nDoc.reference.collection("comments")
                    val commentsSnapshot = commentsColl.get().await()
                    commentsSnapshot.documents.forEach { cDoc ->
                        val data = cDoc.data ?: return@forEach
                        val updates = mutableMapOf<String, Any>()
                        if (data.containsKey("edited")) {
                            updates["isEdited"] = data["edited"]!!
                            updates["edited"] = com.google.firebase.firestore.FieldValue.delete()
                        }
                        if (data.containsKey("pending")) {
                            updates["isPending"] = data["pending"]!!
                            updates["pending"] = com.google.firebase.firestore.FieldValue.delete()
                        }
                        if (updates.isNotEmpty()) {
                            cDoc.reference.update(updates).await()
                            count++
                        }
                    }

                    // Checklist
                    val checklistColl = nDoc.reference.collection("checklist")
                    val checklistSnapshot = checklistColl.get().await()
                    checklistSnapshot.documents.forEach { clDoc ->
                        val data = clDoc.data ?: return@forEach
                        val updates = mutableMapOf<String, Any>()
                        if (data.containsKey("checked")) {
                            updates["isChecked"] = data["checked"]!!
                            updates["checked"] = com.google.firebase.firestore.FieldValue.delete()
                        }
                        if (updates.isNotEmpty()) {
                            clDoc.reference.update(updates).await()
                            count++
                        }
                    }
                }

                // Cleaning Tasks
                val cleaningColl = gDoc.reference.collection("cleaning")
                val cleaningSnapshot = cleaningColl.get().await()
                cleaningSnapshot.documents.forEach { tDoc ->
                    val data = tDoc.data ?: return@forEach
                    val updates = mutableMapOf<String, Any>()
                    if (data.containsKey("checked")) {
                        updates["isChecked"] = data["checked"]!!
                        updates["checked"] = com.google.firebase.firestore.FieldValue.delete()
                    }
                    if (data.containsKey("enabled")) {
                        updates["isEnabled"] = data["enabled"]!!
                        updates["enabled"] = com.google.firebase.firestore.FieldValue.delete()
                    }
                    if (data.containsKey("recurring")) {
                        updates["isRecurring"] = data["recurring"]!!
                        updates["recurring"] = com.google.firebase.firestore.FieldValue.delete()
                    }
                    if (updates.isNotEmpty()) {
                        tDoc.reference.update(updates).await()
                        count++
                    }
                }

                // Shopping Items
                val shoppingColl = gDoc.reference.collection("shopping")
                val shoppingSnapshot = shoppingColl.get().await()
                shoppingSnapshot.documents.forEach { sDoc ->
                    val data = sDoc.data ?: return@forEach
                    val updates = mutableMapOf<String, Any>()
                    if (data.containsKey("checked")) {
                        updates["isChecked"] = data["checked"]!!
                        updates["checked"] = com.google.firebase.firestore.FieldValue.delete()
                    }
                    if (updates.isNotEmpty()) {
                        sDoc.reference.update(updates).await()
                        count++
                    }
                }
                
                onProgress("Groups: ${gIndex + 1}/$totalGroups (Updates: $count)")
            }
            
            onProgress("Migration Finished. Total updates: $count")
            true
        } catch (e: Exception) {
            android.util.Log.e("UserAdmin", "Fields migration failed", e)
            false
        }
    }
}
