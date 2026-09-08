package com.jtool.android.core.sync.helpers

import android.content.Context
import android.util.Log
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
import com.jtool.android.core.models.user.UserData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source

class GroupOperationsHelper(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsManager,
    private val userAdminHelperProvider: () -> UserAdminHelper,
) {
    private fun getUsersColl() = firestore.collection("users")
    private fun getGroupsColl() = firestore.collection("groups")
    private fun getInvitesColl() = firestore.collection("invites")
    private fun getUserDataColl() = firestore.collection("user_data")
    private fun getGroupNotificationsColl(gid: String) = getGroupsColl().document(gid).collection("group_notifications")

    fun currentGroupIdFlow(): Flow<String?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val listener = getUserDataColl().document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("jTool", "Error in currentGroupIdFlow: ${error.message}")
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

    suspend fun createGroup(groupName: String, isPrivate: Boolean = false): String? {
        val uid = auth.currentUser?.uid ?: return null
        val newGroup = GroupInfo(
            name = groupName, 
            ownerUid = uid, 
            createdAt = System.currentTimeMillis(),
            isPrivate = isPrivate
        )
        return try {
            val groupDoc = getGroupsColl().add(newGroup).await()
            val groupId = groupDoc.id
            groupDoc.collection("members").document(uid).set(MemberInfo(joinedAt = System.currentTimeMillis())).await()
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
                    isGroupLevel = true
                )
                getGroupNotificationsColl(groupId).document(notification.id).set(notification).await()
            }
            
            settings.addGroup(newGroup.copy(id = groupId))
            settings.setCurrentGroupId(groupId)
            userAdminHelperProvider().pushUserSettings()
            groupId
        } catch (e: Exception) { null }
    }

    suspend fun ensurePrivateGroupExists(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        
        // 1. Check server-side for any group where user is owner and it's private
        val privateGroups = getGroupsColl()
            .whereEqualTo(GroupInfo::ownerUid.name, uid)
            .whereEqualTo(GroupInfo::isPrivate.name, true)
            .limit(1)
            .get()
            .await()
            
        if (!privateGroups.isEmpty) {
            val group = privateGroups.documents.first()
            val gid = group.id
            
            // Ensure it's in the user's group list and settings
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
        
        // 2. Not found, create it
        val newId = createGroup(Loc.Settings.privateGroup, isPrivate = true)
        return newId != null
    }

    suspend fun updateGroupName(groupId: String, newName: String): Boolean {
        return try {
            val groupRef = getGroupsColl().document(groupId)
            // Allow renaming private groups by owner
            groupRef.update(GroupInfo::name.name, newName).await()
            val groups = settings.getGroups().toMutableMap()
            groups[groupId]?.let { 
                groups[groupId] = it.copy(name = newName)
                settings.saveGroups(groups) 
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun joinGroup(groupId: String, isInviteAcceptance: Boolean = false): Pair<JoinGroupResultEnum, GroupInfo?> {
        val uid = auth.currentUser?.uid ?: return JoinGroupResultEnum.FAILED to null
        val trimmedGroupId = groupId.trim()
        
        val alreadyMember = try { 
            getGroupsColl().document(trimmedGroupId).collection("members").document(uid).get().await().exists() 
        } catch (e: Exception) { false }
        
        if (alreadyMember) return JoinGroupResultEnum.ALREADY_MEMBER to null
        
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: ""
        val hasInvite = isInviteAcceptance || try { 
            getInvitesColl().document(email).collection("items").document(trimmedGroupId).get().await().exists() 
        } catch (e: Exception) { false }
        
        if (!hasInvite) {
            val groupData = getGroupsColl().document(trimmedGroupId).get().await().toObject(GroupInfo::class.java)
            if (groupData?.isPrivate == true) return JoinGroupResultEnum.FAILED to null

            val existing = try { 
                getGroupsColl().document(trimmedGroupId).collection("join_requests").document(uid).get().await().exists() 
            } catch (e: Exception) { false }
            
            if (existing) return JoinGroupResultEnum.ALREADY_REQUESTED to null
            return if (sendJoinRequest(trimmedGroupId)) JoinGroupResultEnum.REQUEST_SENT to null else JoinGroupResultEnum.FAILED to null
        }
        
        return try {
            clearLocalCache()
            val groupRef = getGroupsColl().document(trimmedGroupId)
            groupRef.collection("members").document(uid).set(MemberInfo(joinedAt = System.currentTimeMillis())).await()
            
            val groupSnapshot = groupRef.get().await()
            val group = groupSnapshot.toObject(GroupInfo::class.java)?.copy(id = groupSnapshot.id)
            val groupName = group?.name ?: "New Group"
            
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            val user = userSnapshot.toObject(User::class.java)
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
                excludeUid = uid
            )
            getGroupNotificationsColl(trimmedGroupId).document(notification.id).set(notification).await()
            
            userAdminHelperProvider().pushUserSettings()
            JoinGroupResultEnum.JOINED to finalGroup
        } catch (e: Exception) { JoinGroupResultEnum.FAILED to null }
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
                
                // Update user document FIRST to trigger flow emissions and stop listeners
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
                // Then delete member record (revokes permission)
                groupDoc.collection("members").document(uid).delete().await()
                
                val notification = UserNotification(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId, 
                    title = Loc.Settings.memberLeft, 
                    message = Loc.Settings.memberLeftMsg.format(settings.getUserName()), 
                    timestamp = System.currentTimeMillis(), 
                    type = NotificationTypeEnum.INFO, 
                    isGroupLevel = true
                )
                getGroupNotificationsColl(groupId).document(notification.id).set(notification).await()
            }
        } catch (e: Exception) {
            Log.e("jTool", "Error leaving group: ${e.message}")
        }
    }

    suspend fun switchGroup(groupId: String) {
        val uid = auth.currentUser?.uid ?: return
        Log.d("jTool", "Switching group to: $groupId")
        clearLocalCache()
        getUserDataColl().document(uid).update(UserData::currentGroupId.name, groupId).await()
        settings.setCurrentGroupId(groupId)
        userAdminHelperProvider().pushUserSettings()
        Log.d("jTool", "Group switched and settings pushed")
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
            
            getInvitesColl().document(sanitizedEmail).collection("items").document(groupId).set(
                mapOf(
                    com.jtool.android.core.models.group.Invite::groupId.name to groupId,
                    com.jtool.android.core.models.group.Invite::groupName.name to groupName,
                    com.jtool.android.core.models.group.Invite::fromEmail.name to (auth.currentUser?.email ?: "Unknown"),
                    "inviterUid" to (auth.currentUser?.uid ?: ""),
                    com.jtool.android.core.models.group.Invite::timestamp.name to System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Exception) { false }
    }

    fun invitesFlow(): Flow<List<com.jtool.android.core.models.group.Invite>> = callbackFlow {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: run { close(); return@callbackFlow }
        val listener = getInvitesColl().document(email).collection("items").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("jTool", "Error in invitesFlow: ${error.message}")
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
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
                val (result, _) = joinGroup(groupId, isInviteAcceptance = true)
                val success = result == JoinGroupResultEnum.JOINED
                if (success) getInvitesColl().document(email).collection("items").document(inviteId).delete().await()
                success
            } else {
                val inviteSnap = getInvitesColl().document(email).collection("items").document(inviteId).get().await()
                val groupName = inviteSnap.getString("groupName") ?: "group"
                val inviterUid = inviteSnap.getString("inviterUid")
                getInvitesColl().document(email).collection("items").document(inviteId).delete().await()
                
                if (!inviterUid.isNullOrEmpty()) {
                    val notification = UserNotification(
                        id = UUID.randomUUID().toString(),
                        title = Loc.Settings.invitationRejected, 
                        message = Loc.Settings.invitationRejectedMsg.format(email, groupName), 
                        timestamp = System.currentTimeMillis(), 
                        type = NotificationTypeEnum.DECLINED
                    )
                    getUserDataColl().document(inviterUid).update("notifications", FieldValue.arrayUnion(notification)).await()
                }
                true
            }
        } catch (e: Exception) { false }
    }

    fun groupMembersFlow(groupId: String): Flow<List<String>> = callbackFlow {
        val listener = getGroupsColl().document(groupId).collection("members").addSnapshotListener { snapshot, error -> 
            if (error != null) {
                Log.e("jTool", "Error in groupMembersFlow for $groupId: ${error.message}")
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot.documents.map { it.id }) 
        }
        awaitClose { listener.remove() }
    }

    fun userGroupsFlow(): Flow<List<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = getUsersColl().document(uid).addSnapshotListener { snapshot, error -> 
            if (error != null) {
                Log.e("jTool", "Error in userGroupsFlow: ${error.message}")
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
                Log.e("jTool", "Error in groupInfoFlow: ${error.message}")
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
        val listener = getGroupsColl().document(groupId).collection("join_requests").addSnapshotListener { snapshot, error -> 
            if (error != null) {
                Log.e("jTool", "Error in joinRequestsFlow for $groupId: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
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
                id = UUID.randomUUID().toString(),
                groupId = request.groupId, 
                title = Loc.Settings.memberJoined, 
                message = Loc.Settings.memberJoinedMsg.format(request.userName), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.INFO, 
                isGroupLevel = true,
                excludeUid = request.userId
            )
            getGroupNotificationsColl(request.groupId).document(groupNotif.id).set(groupNotif).await()
            
            val personalNotif = UserNotification(
                id = UUID.randomUUID().toString(),
                title = Loc.Settings.requestApproved, 
                message = Loc.Settings.addedToGroup.format(groupName), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.ACCEPTED
            )
            getUserDataColl().document(request.userId).update("notifications", FieldValue.arrayUnion(personalNotif)).await()
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
                id = UUID.randomUUID().toString(),
                title = Loc.Settings.requestDeclined, 
                message = Loc.Settings.requestToJoinDeclined.format(groupName), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.DECLINED
            )
            getUserDataColl().document(request.userId).update("notifications", FieldValue.arrayUnion(notification)).await()
            true
        } catch (e: Exception) { false }
    }

    fun personalNotificationsFlow(): Flow<List<UserNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { close(); return@callbackFlow }
        val listener = getUserDataColl().document(uid).addSnapshotListener { snapshot, error -> 
            if (error != null) {
                Log.e("jTool", "Error in personalNotificationsFlow: ${error.message}")
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val data = snapshot.toObject(UserData::class.java)
                trySend(data?.notifications ?: emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    fun groupNotificationsFlow(groupId: String): Flow<List<UserNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = getGroupNotificationsColl(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("jTool", "Error in groupNotificationsFlow for $groupId: ${error.message}")
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val allNotifs = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserNotification::class.java)?.copy(id = doc.id, isGroupLevel = true, groupId = groupId)
                }
                
                launch {
                    val filtered = allNotifs.filter { notif ->
                        if (notif.excludeUid == uid) return@filter false
                        
                        val seen = try {
                            getGroupNotificationsColl(groupId).document(notif.id)
                                .collection("seen_by").document(uid).get(Source.CACHE).await().exists()
                        } catch (e: Exception) {
                            try {
                                getGroupNotificationsColl(groupId).document(notif.id)
                                    .collection("seen_by").document(uid).get().await().exists()
                            } catch (e2: Exception) { false }
                        }
                        !seen
                    }
                    trySend(filtered)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun markNotificationAsRead(notification: UserNotification) {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: ""
        Log.d("jTool", "Marking notification as read: ${notification.id}, type: ${notification.type}, isGroupLevel: ${notification.isGroupLevel}")
        
        try {
            when {
                notification.type == NotificationTypeEnum.INVITE && notification.groupId != null -> {
                    getInvitesColl().document(email).collection("items").document(notification.id).delete().await()
                    Log.d("jTool", "Invite dismissed: ${notification.id}")
                }
                notification.type == NotificationTypeEnum.JOIN_REQUEST && notification.groupId != null -> {
                    getGroupsColl().document(notification.groupId).collection("join_requests").document(notification.id).delete().await()
                    Log.d("jTool", "Join request dismissed: ${notification.id}")
                }
                notification.isGroupLevel && notification.groupId != null -> {
                    val notifRef = getGroupNotificationsColl(notification.groupId).document(notification.id)
                    notifRef.collection("seen_by").document(uid).set(mapOf("seen" to true)).await()
                    Log.d("jTool", "Group notification seen updated for uid: $uid")
                    
                    val membersSnapshot = getGroupsColl().document(notification.groupId).collection("members").get().await()
                    val membersCount = membersSnapshot.size()
                    val seenSnapshot = notifRef.collection("seen_by").get().await()
                    val seenCount = seenSnapshot.size()
                    
                    Log.d("jTool", "Group notification seenCount: $seenCount / $membersCount")
                    
                    if (seenCount >= membersCount) {
                        notifRef.delete().await()
                        Log.d("jTool", "Group notification deleted as all members saw it")
                    } else {
                        notifRef.update("lastSeenUpdate", System.currentTimeMillis()).await()
                    }
                }
                else -> {
                    val userRef = getUserDataColl().document(uid)
                    val snapshot = userRef.get(Source.SERVER).await()
                    val data = snapshot.toObject(UserData::class.java)
                    val currentNotifs = data?.notifications ?: emptyList()
                    
                    // Filter out the specific notification
                    val updatedNotifs = currentNotifs.filter { 
                        if (notification.id.isNotEmpty()) it.id != notification.id
                        else it.timestamp != notification.timestamp || it.title != notification.title
                    }
                    
                    if (currentNotifs.size != updatedNotifs.size) {
                        Log.d("jTool", "Updating personal notifications. Count: ${currentNotifs.size} -> ${updatedNotifs.size}")
                        userRef.update("notifications", updatedNotifs).await()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("jTool", "Failed to mark notification as read", e)
        }
    }
}
