package com.jtool.android.core.sync.helpers

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.group.MemberInfo
import com.jtool.android.core.models.group.enums.JoinGroupResultEnum
import com.jtool.android.core.models.user.User
import com.jtool.android.core.models.user.UserData
import com.jtool.android.core.settings.SettingsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GroupOperationsHelper(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsManager,
    private val notificationHelper: NotificationHelper,
    private val userAdminHelperProvider: () -> UserAdminHelper
) {
    private fun getUsersColl() = firestore.collection("users")
    private fun getGroupsColl() = firestore.collection("groups")
    private fun getUserDataColl() = firestore.collection("user_data")

    fun currentGroupIdFlow(): Flow<String?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val listener = getUserDataColl().document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val data = snapshot.toObject(UserData::class.java)
                trySend(data?.currentGroupId)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getUserGroups(): List<String> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snapshot = getUsersColl().document(uid).get().await()
        val user = snapshot.toObject(User::class.java)
        return user?.groupIds ?: emptyList()
    }

    fun userGroupsFlow(): Flow<List<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = getUsersColl().document(uid).addSnapshotListener { snapshot, error -> 
            if (error != null) {
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) { 
                @Suppress("UNCHECKED_CAST")
                trySend(snapshot.get(User::groupIds.name) as? List<String> ?: emptyList()) 
            } 
        }
        awaitClose { listener.remove() }
    }

    fun groupInfoFlow(groupIds: List<String>): Flow<Map<String, GroupInfo>> = callbackFlow {
        if (groupIds.isEmpty()) { trySend(emptyMap()); awaitClose { }; return@callbackFlow }
        val listener = getGroupsColl().whereIn(FieldPath.documentId(), groupIds.take(30)).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyMap())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(
                    snapshot.documents.associateBy(
                        keySelector = { it.id },
                        valueTransform = { doc -> 
                            (doc.toObject(GroupInfo::class.java) ?: GroupInfo()).copy(id = doc.id) 
                        }
                    )
                )
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun createGroup(groupName: String, isPrivate: Boolean = false, isInviteOnly: Boolean = false): String? {
        val uid = auth.currentUser?.uid ?: return null
        val newGroup = GroupInfo(
            name = groupName, 
            ownerUid = uid, 
            createdAt = System.currentTimeMillis(),
            isPrivate = isPrivate,
            isInviteOnly = isInviteOnly
        )
        return try {
            val groupDoc = getGroupsColl().add(newGroup).await()
            val groupId = groupDoc.id
            val calEmail = settings.getGoogleCalendarEmail()
            groupDoc.collection("members").document(uid).set(MemberInfo(joinedAt = System.currentTimeMillis(), calendarEmail = calEmail)).await()
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            val user = userSnapshot.toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            if (!currentGroups.contains(groupId)) {
                if (isPrivate) currentGroups.add(0, groupId) else currentGroups.add(groupId)
            }
            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            getUserDataColl().document(uid).update(UserData::currentGroupId.name, groupId).await()
            
            if (!isPrivate) {
                val notification = UserNotification(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId, 
                    title = Loc.Settings.groupCreatedNotif, 
                    message = Loc.Settings.groupCreatedMsg.format(groupName), 
                    timestamp = System.currentTimeMillis(), 
                    type = NotificationTypeEnum.INFO, 
                    isGroupLevel = true,
                    titleKey = "settings_groupCreatedNotif",
                    messageKey = "settings_groupCreatedMsg",
                    messageArgs = listOf(groupName)
                )
                notificationHelper.sendGroupLevelNotification(groupId, notification)
            }
            
            settings.addGroup(newGroup.copy(id = groupId))
            settings.setCurrentGroupId(groupId)
            userAdminHelperProvider().pushUserSettings()
            groupId
        } catch (e: Exception) { null }
    }

    suspend fun ensurePrivateGroupExists(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val privateGroups = getGroupsColl()
            .whereEqualTo(GroupInfo::ownerUid.name, uid)
            .whereEqualTo(GroupInfo::isPrivate.name, true)
            .limit(1)
            .get()
            .await()
            
        if (!privateGroups.isEmpty) {
            val group = privateGroups.documents.first()
            val gid = group.id
            val currentGroups = getUserGroups()
            if (!currentGroups.contains(gid)) {
                val updated = currentGroups.toMutableList().apply { add(0, gid) }
                getUsersColl().document(uid).update(User::groupIds.name, updated).await()
            }
            if (settings.getGroups()[gid] == null) {
                val groupInfo = (group.toObject(GroupInfo::class.java) ?: GroupInfo()).copy(id = gid)
                settings.addGroup(groupInfo)
            }
            return false 
        }
        val newId = createGroup(Loc.Settings.privateGroup, isPrivate = true)
        return newId != null
    }

    suspend fun updateGroupName(groupId: String, newName: String): Boolean {
        return try {
            val groupRef = getGroupsColl().document(groupId)
            groupRef.update(GroupInfo::name.name, newName).await()
            val groups = settings.getGroups().toMutableMap()
            groups[groupId]?.let { 
                groups[groupId] = it.copy(name = newName)
                settings.saveGroups(groups) 
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateGroupInviteOnly(groupId: String, isInviteOnly: Boolean): Boolean {
        return try {
            val groupRef = getGroupsColl().document(groupId)
            groupRef.update(GroupInfo::isInviteOnly.name, isInviteOnly).await()
            val groups = settings.getGroups().toMutableMap()
            groups[groupId]?.let { 
                groups[groupId] = it.copy(isInviteOnly = isInviteOnly)
                settings.saveGroups(groups) 
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun joinGroup(groupId: String, isInviteAcceptance: Boolean = false): Pair<JoinGroupResultEnum, GroupInfo?> {
        val uid = auth.currentUser?.uid ?: return JoinGroupResultEnum.FAILED to null
        val trimmedGroupId = groupId.trim()
        val memberDoc = getGroupsColl().document(trimmedGroupId).collection("members").document(uid).get().await()
        if (memberDoc.exists()) return JoinGroupResultEnum.ALREADY_MEMBER to null
        
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: ""
        val hasInvite = isInviteAcceptance || firestore.collection("invites").document(email).collection("items").document(trimmedGroupId).get().await().exists()
        
        if (!hasInvite) {
            val groupData = getGroupsColl().document(trimmedGroupId).get().await().toObject(GroupInfo::class.java)
            if (groupData == null || groupData.isPrivate || groupData.isInviteOnly) return JoinGroupResultEnum.FAILED to null
            val existingReq = getGroupsColl().document(trimmedGroupId).collection("join_requests").document(uid).get().await()
            if (existingReq.exists()) return JoinGroupResultEnum.ALREADY_REQUESTED to null
            val groupMembershipHelper = GroupMembershipHelper(firestore, auth, settings, notificationHelper)
            val sent = groupMembershipHelper.sendJoinRequest(trimmedGroupId)
            if (sent) return JoinGroupResultEnum.REQUEST_SENT to null
            return JoinGroupResultEnum.FAILED to null 
        }
        
        return try {
            clearLocalCache()
            val groupRef = getGroupsColl().document(trimmedGroupId)
            val calEmail = settings.getGoogleCalendarEmail()
            groupRef.collection("members").document(uid).set(MemberInfo(joinedAt = System.currentTimeMillis(), calendarEmail = calEmail)).await()
            
            val groupSnapshot = groupRef.get().await()
            val group = groupSnapshot.toObject(GroupInfo::class.java)?.copy(id = groupSnapshot.id)
            val groupName = group?.name ?: "New Group"
            
            val userRef = getUsersColl().document(uid)
            val user = userRef.get().await().toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            if (!currentGroups.contains(trimmedGroupId)) currentGroups.add(trimmedGroupId)
            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            getUserDataColl().document(uid).update(UserData::currentGroupId.name, trimmedGroupId).await()
            
            val finalGroup = GroupInfo(id = trimmedGroupId, name = groupName, ownerUid = group?.ownerUid)
            settings.addGroup(finalGroup)
            settings.setCurrentGroupId(trimmedGroupId)
            
            val notification = UserNotification(
                id = UUID.randomUUID().toString(),
                groupId = trimmedGroupId, 
                title = Loc.Settings.memberJoined, 
                message = Loc.Settings.memberJoinedMsg.format(settings.getUserName()), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.INFO, 
                isGroupLevel = true,
                excludeUid = uid,
                titleKey = "settings_memberJoined",
                messageKey = "settings_memberJoinedMsg",
                messageArgs = listOf(settings.getUserName())
            )
            notificationHelper.sendGroupLevelNotification(trimmedGroupId, notification)
            userAdminHelperProvider().pushUserSettings()
            JoinGroupResultEnum.JOINED to finalGroup
        } catch (e: Exception) { JoinGroupResultEnum.FAILED to null }
    }

    suspend fun switchGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        clearLocalCache()
        getUserDataColl().document(uid).update(UserData::currentGroupId.name, groupId).await()
        settings.setCurrentGroupId(groupId)
        userAdminHelperProvider().pushUserSettings()
    }

    fun clearLocalCache() {
        listOf(PersistencePrefixEnum.SHOPPING, PersistencePrefixEnum.NOTES, PersistencePrefixEnum.LINKS, PersistencePrefixEnum.CLEANING).forEach {
            context.getSharedPreferences(it.prefix, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

    suspend fun leaveGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            val user = userSnapshot.toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            
            if (currentGroups.contains(groupId)) {
                currentGroups.remove(groupId)
                val nextGroupId = if (settings.getCurrentGroupId() == groupId) currentGroups.firstOrNull() else settings.getCurrentGroupId()
                
                userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
                getUserDataColl().document(uid).update(UserData::currentGroupId.name, nextGroupId).await()
                
                clearLocalCache()
                settings.removeGroup(groupId)
                if (settings.getCurrentGroupId() == groupId) settings.setCurrentGroupId(nextGroupId)
                userAdminHelperProvider().pushUserSettings()
            }

            val groupDoc = getGroupsColl().document(groupId)
            val membersSnapshot = groupDoc.collection("members").get().await()
            val memberIds = membersSnapshot.documents.map { it.id }
            
            if ((memberIds.size <= 1) && memberIds.contains(uid)) {
                userAdminHelperProvider().deleteGroupPermanently(groupId)
            } else {
                val groupSnapshot = groupDoc.get().await()
                val group = groupSnapshot.toObject(GroupInfo::class.java)
                if (group?.ownerUid == uid) {
                    val nextOwner = memberIds.filter { it != uid }.random()
                    groupDoc.update(GroupInfo::ownerUid.name, nextOwner).await()
                }
                groupDoc.collection("members").document(uid).delete().await()
                
                val notification = UserNotification(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId, 
                    title = Loc.Settings.memberLeft, 
                    message = Loc.Settings.memberLeftMsg.format(settings.getUserName()), 
                    timestamp = System.currentTimeMillis(), 
                    type = NotificationTypeEnum.INFO, 
                    isGroupLevel = true,
                    titleKey = "settings_memberLeft",
                    messageKey = "settings_memberLeftMsg",
                    messageArgs = listOf(settings.getUserName())
                )
                notificationHelper.sendGroupLevelNotification(groupId, notification)
            }
        } catch (e: Exception) {
            Log.e("jTool", "Error leaving group: ${e.message}")
        }
    }
}
