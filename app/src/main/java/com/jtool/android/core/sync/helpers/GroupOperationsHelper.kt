package com.jtool.android.core.sync.helpers

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.user.User
import com.jtool.android.core.models.group.MemberInfo
import com.jtool.android.core.models.group.enums.JoinGroupResultEnum
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope

class GroupOperationsHelper(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsManager,
    private val userAdminHelperProvider: () -> UserAdminHelper
) {
    private fun getUsersColl() = firestore.collection("users")
    private fun getGroupsColl() = firestore.collection("groups")
    private fun getInvitesColl() = firestore.collection("invites")
    private fun getUserDataColl() = firestore.collection("user_data")
    private fun getPersonalNotificationsColl(uid: String) = getUserDataColl().document(uid).collection("personal_notifications")
    private fun getGroupNotificationsColl(gid: String) = getGroupsColl().document(gid).collection("group_notifications")

    fun currentGroupIdFlow(): Flow<String?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val listener = getUserDataColl().document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val data = snapshot.toObject(com.jtool.android.core.models.user.UserData::class.java)
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

    suspend fun createGroup(groupName: String): String? {
        val uid = auth.currentUser?.uid ?: return null
        val newGroup = GroupInfo(name = groupName, ownerUid = uid, createdAt = System.currentTimeMillis())
        return try {
            val groupDoc = getGroupsColl().add(newGroup).await()
            val groupId = groupDoc.id
            groupDoc.collection("members").document(uid).set(MemberInfo(joinedAt = System.currentTimeMillis())).await()
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            val user = userSnapshot.toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            if (!currentGroups.contains(groupId)) currentGroups.add(groupId)
            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            getUserDataColl().document(uid).update(com.jtool.android.core.models.user.UserData::currentGroupId.name, groupId).await()
            
            val notification = UserNotification(
                groupId = groupId, 
                title = Loc.settings_groupCreatedNotif, 
                message = Loc.settings_groupCreatedMsg.format(groupName), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.INFO, 
                isGroupLevel = true
            )
            getGroupNotificationsColl(groupId).add(notification).await()
            
            settings.addGroup(newGroup.copy(id = groupId))
            settings.setCurrentGroupId(groupId)
            userAdminHelperProvider().pushUserSettings()
            groupId
        } catch (e: Exception) { null }
    }

    suspend fun updateGroupName(groupId: String, newName: String): Boolean {
        return try {
            getGroupsColl().document(groupId).update(GroupInfo::name.name, newName).await()
            val groups = settings.getGroups().toMutableMap()
            groups[groupId]?.let { 
                groups[groupId] = it.copy(name = newName)
                settings.saveGroups(groups) 
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun joinGroup(groupId: String, isInviteAcceptance: Boolean = false): JoinGroupResultEnum {
        val uid = auth.currentUser?.uid ?: return JoinGroupResultEnum.FAILED
        val trimmedGroupId = groupId.trim()
        
        val alreadyMember = try { 
            getGroupsColl().document(trimmedGroupId).collection("members").document(uid).get().await().exists() 
        } catch (e: Exception) { false }
        
        if (alreadyMember) return JoinGroupResultEnum.ALREADY_MEMBER
        
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: ""
        val hasInvite = isInviteAcceptance || try { 
            getInvitesColl().document(email).collection("items").document(trimmedGroupId).get().await().exists() 
        } catch (e: Exception) { false }
        
        if (!hasInvite) {
            val existing = try { 
                getGroupsColl().document(trimmedGroupId).collection("join_requests").document(uid).get().await().exists() 
            } catch (e: Exception) { false }
            
            if (existing) return JoinGroupResultEnum.ALREADY_REQUESTED
            return if (sendJoinRequest(trimmedGroupId)) JoinGroupResultEnum.REQUEST_SENT else JoinGroupResultEnum.FAILED
        }
        
        return try {
            clearLocalCache()
            val groupRef = getGroupsColl().document(trimmedGroupId)
            groupRef.collection("members").document(uid).set(MemberInfo(joinedAt = System.currentTimeMillis())).await()
            
            val groupSnapshot = groupRef.get().await()
            val group = groupSnapshot.toObject(GroupInfo::class.java)?.copy(id = groupSnapshot.id)
            
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            val user = userSnapshot.toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            if (!currentGroups.contains(trimmedGroupId)) currentGroups.add(trimmedGroupId)
            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            getUserDataColl().document(uid).update(com.jtool.android.core.models.user.UserData::currentGroupId.name, trimmedGroupId).await()
            
            settings.addGroup(GroupInfo(id = trimmedGroupId, name = group?.name ?: "New Group", ownerUid = group?.ownerUid))
            settings.setCurrentGroupId(trimmedGroupId)
            
            val notification = UserNotification(
                groupId = trimmedGroupId, 
                title = Loc.settings_memberJoined, 
                message = Loc.settings_memberJoinedMsg.format(settings.getUserName()), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.INFO, 
                isGroupLevel = true
            )
            getGroupNotificationsColl(trimmedGroupId).add(notification).await()
            
            userAdminHelperProvider().pushUserSettings()
            JoinGroupResultEnum.JOINED
        } catch (e: Exception) { JoinGroupResultEnum.FAILED }
    }

    suspend fun leaveGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val groupDoc = getGroupsColl().document(groupId)
            val membersSnapshot = groupDoc.collection("members").get().await()
            val memberIds = membersSnapshot.documents.map { it.id }
            
            if (memberIds.size <= 1 && memberIds.contains(uid)) {
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
                    groupId = groupId, 
                    title = Loc.settings_memberLeft, 
                    message = Loc.settings_memberLeftMsg.format(settings.getUserName()), 
                    timestamp = System.currentTimeMillis(), 
                    type = NotificationTypeEnum.INFO, 
                    isGroupLevel = true
                )
                getGroupNotificationsColl(groupId).add(notification).await()
            }
            
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            val user = userSnapshot.toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            currentGroups.remove(groupId)
            val nextGroupId = if (settings.getCurrentGroupId() == groupId) currentGroups.firstOrNull() else settings.getCurrentGroupId()
            
            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            getUserDataColl().document(uid).update(com.jtool.android.core.models.user.UserData::currentGroupId.name, nextGroupId).await()
            
            clearLocalCache()
            settings.removeGroup(groupId)
            if (settings.getCurrentGroupId() == groupId) settings.setCurrentGroupId(nextGroupId)
            userAdminHelperProvider().pushUserSettings()
        } catch (e: Exception) {}
    }

    suspend fun switchGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        clearLocalCache()
        getUserDataColl().document(uid).update(com.jtool.android.core.models.user.UserData::currentGroupId.name, groupId).await()
        settings.setCurrentGroupId(groupId)
        userAdminHelperProvider().pushUserSettings()
    }

    fun clearLocalCache() {
        listOf(PersistencePrefixEnum.SHOPPING, PersistencePrefixEnum.NOTES, PersistencePrefixEnum.LINKS, PersistencePrefixEnum.CLEANING).forEach {
            context.getSharedPreferences(it.prefix, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

    suspend fun sendInvite(email: String, groupId: String): Boolean {
        val sanitizedEmail = email.lowercase().trim()
        return try {
            val groupName = try { 
                getGroupsColl().document(groupId).get().await().toObject(GroupInfo::class.java)?.name 
            } catch (e: Exception) { null } ?: settings.getGroups()[groupId]?.name ?: "New Group"
            
            getInvitesColl().document(sanitizedEmail).collection("items").document(groupId).set(mapOf(
                com.jtool.android.core.models.group.Invite::groupId.name to groupId,
                com.jtool.android.core.models.group.Invite::groupName.name to groupName,
                com.jtool.android.core.models.group.Invite::fromEmail.name to (auth.currentUser?.email ?: "Unknown"),
                "inviterUid" to (auth.currentUser?.uid ?: ""),
                com.jtool.android.core.models.group.Invite::timestamp.name to System.currentTimeMillis()
            )).await()
            true
        } catch (e: Exception) { false }
    }

    fun invitesFlow(): Flow<List<com.jtool.android.core.models.group.Invite>> = callbackFlow {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: run { close(); return@callbackFlow }
        val listener = getInvitesColl().document(email).collection("items").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject(com.jtool.android.core.models.group.Invite::class.java)?.copy(inviteId = it.id) })
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean): Boolean {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: return false
        return try {
            if (accept) {
                val success = joinGroup(groupId, isInviteAcceptance = true) == JoinGroupResultEnum.JOINED
                if (success) getInvitesColl().document(email).collection("items").document(inviteId).delete().await()
                success
            } else {
                val inviteSnap = getInvitesColl().document(email).collection("items").document(inviteId).get().await()
                val groupName = inviteSnap.getString("groupName") ?: "group"
                val inviterUid = inviteSnap.getString("inviterUid")
                getInvitesColl().document(email).collection("items").document(inviteId).delete().await()
                
                if (inviterUid != null && inviterUid.isNotEmpty()) {
                    val notification = UserNotification(
                        title = Loc.settings_invitationRejected, 
                        message = Loc.settings_invitationRejectedMsg.format(email, groupName), 
                        timestamp = System.currentTimeMillis(), 
                        type = NotificationTypeEnum.DECLINED
                    )
                    getPersonalNotificationsColl(inviterUid).add(notification).await()
                }
                true
            }
        } catch (e: Exception) { false }
    }

    fun groupMembersFlow(groupId: String): Flow<List<String>> = callbackFlow {
        val listener = getGroupsColl().document(groupId).collection("members").addSnapshotListener { snapshot, _ -> 
            if (snapshot != null) trySend(snapshot.documents.map { it.id }) 
        }
        awaitClose { listener.remove() }
    }

    fun userGroupsFlow(): Flow<List<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = getUsersColl().document(uid).addSnapshotListener { snapshot, _ -> 
            if (snapshot != null) { 
                @Suppress("UNCHECKED_CAST")
                trySend(snapshot.get(User::groupIds.name) as? List<String> ?: emptyList()) 
            } 
        }
        awaitClose { listener.remove() }
    }

    fun groupInfoFlow(groupIds: List<String>): Flow<Map<String, GroupInfo>> = callbackFlow {
        if (groupIds.isEmpty()) { trySend(emptyMap()); awaitClose { }; return@callbackFlow }
        val listener = getGroupsColl().whereIn(FieldPath.documentId(), groupIds.take(30)).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                trySend(snapshot.documents.associateBy({ it.id }, { doc -> 
                    (doc.toObject(GroupInfo::class.java) ?: GroupInfo()).copy(id = doc.id) 
                }))
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendJoinRequest(groupId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val request = com.jtool.android.core.models.group.JoinRequest(
                groupId = groupId, 
                userId = uid, 
                userName = settings.getUserName(), 
                userEmail = auth.currentUser?.email ?: "", 
                timestamp = System.currentTimeMillis()
            )
            getGroupsColl().document(groupId).collection("join_requests").document(uid).set(request).await()
            true
        } catch (e: Exception) { false }
    }

    fun joinRequestsFlow(groupId: String): Flow<List<com.jtool.android.core.models.group.JoinRequest>> = callbackFlow {
        val listener = getGroupsColl().document(groupId).collection("join_requests").addSnapshotListener { snapshot, _ -> 
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject(com.jtool.android.core.models.group.JoinRequest::class.java) }) 
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun acceptJoinRequest(request: com.jtool.android.core.models.group.JoinRequest): Boolean {
        return try {
            val groupRef = getGroupsColl().document(request.groupId)
            groupRef.collection("members").document(request.userId).set(MemberInfo(joinedAt = System.currentTimeMillis())).await()
            
            val userRef = getUsersColl().document(request.userId)
            val user = userRef.get().await().toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            if (!currentGroups.contains(request.groupId)) currentGroups.add(request.groupId)
            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            
            groupRef.collection("join_requests").document(request.userId).delete().await()
            val groupSnapshot = groupRef.get().await()
            val groupName = groupSnapshot.getString("name") ?: "Group"
            
            val groupNotif = UserNotification(
                groupId = request.groupId, 
                title = Loc.settings_requestApproved, 
                message = Loc.settings_addedToGroup.format(groupName), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.ACCEPTED, 
                isGroupLevel = true
            )
            getGroupNotificationsColl(request.groupId).add(groupNotif).await()
            
            val personalNotif = UserNotification(
                title = Loc.settings_requestApproved, 
                message = Loc.settings_addedToGroup.format(groupName), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.ACCEPTED
            )
            getPersonalNotificationsColl(request.userId).add(personalNotif).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun declineJoinRequest(request: com.jtool.android.core.models.group.JoinRequest): Boolean {
        return try {
            val groupRef = getGroupsColl().document(request.groupId)
            groupRef.collection("join_requests").document(request.userId).delete().await()
            val groupSnapshot = groupRef.get().await()
            val groupName = groupSnapshot.getString("name") ?: "Group"
            
            val notification = UserNotification(
                title = Loc.settings_requestDeclined, 
                message = Loc.settings_requestToJoinDeclined.format(groupName), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.DECLINED
            )
            getPersonalNotificationsColl(request.userId).add(notification).await()
            true
        } catch (e: Exception) { false }
    }

    fun personalNotificationsFlow(): Flow<List<UserNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { close(); return@callbackFlow }
        val listener = getPersonalNotificationsColl(uid).addSnapshotListener { snapshot, _ -> 
            if (snapshot != null) trySend(snapshot.documents.mapNotNull { it.toObject(UserNotification::class.java)?.copy(id = it.id) }) 
        }
        awaitClose { listener.remove() }
    }

    fun groupNotificationsFlow(groupId: String): Flow<List<UserNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { close(); return@callbackFlow }
        val listener = getGroupNotificationsColl(groupId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val allNotifs = snapshot.documents.mapNotNull { it.toObject(UserNotification::class.java)?.copy(id = it.id) }
                MainScope().launch {
                    val filtered = allNotifs.filter { notif -> 
                        !getGroupNotificationsColl(groupId).document(notif.id).collection("seen_by").document(uid).get().await().exists() 
                    }
                    trySend(filtered)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun markNotificationAsRead(notification: UserNotification) {
        val uid = auth.currentUser?.uid ?: return
        try {
            if (notification.isGroupLevel && notification.groupId != null) {
                val notifRef = getGroupNotificationsColl(notification.groupId).document(notification.id)
                notifRef.collection("seen_by").document(uid).set(mapOf("seen" to true)).await()
                
                val membersCount = getGroupsColl().document(notification.groupId).collection("members").get().await().size()
                val seenCount = notifRef.collection("seen_by").get().await().size()
                if (seenCount >= membersCount) {
                    notifRef.delete().await()
                }
            } else {
                getPersonalNotificationsColl(uid).document(notification.id).delete().await()
            }
        } catch (e: Exception) {}
    }
}
