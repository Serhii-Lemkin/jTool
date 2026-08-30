package com.jtool.android.core.sync.helpers

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.user.User
import com.jtool.android.core.models.group.MemberInfo
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
            if (!currentGroups.contains(groupId)) {
                currentGroups.add(groupId)
            }
            
            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            getUserDataColl().document(uid).update(com.jtool.android.core.models.user.UserData::currentGroupId.name, groupId).await()
            
            settings.addGroup(newGroup.copy(id = groupId))
            settings.setCurrentGroupId(groupId)
            
            userAdminHelperProvider().pushUserSettings()
            groupId
        } catch (e: Exception) {
            null
        }
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
        } catch (e: Exception) {
            false
        }
    }

    suspend fun joinGroup(groupId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            clearLocalCache()
            getGroupsColl().document(groupId).collection("members").document(uid).set(MemberInfo(joinedAt = System.currentTimeMillis())).await()
            
            val groupSnapshot = getGroupsColl().document(groupId).get().await()
            val group = groupSnapshot.toObject(GroupInfo::class.java)?.copy(id = groupSnapshot.id)
            val groupName = group?.name ?: "New Group"
            val ownerUid = group?.ownerUid
            
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            val user = userSnapshot.toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            
            if (!currentGroups.contains(groupId)) {
                currentGroups.add(groupId)
            }
            
            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            getUserDataColl().document(uid).update(com.jtool.android.core.models.user.UserData::currentGroupId.name, groupId).await()
            
            settings.addGroup(GroupInfo(id = groupId, name = groupName, ownerUid = ownerUid))
            settings.setCurrentGroupId(groupId)
            
            userAdminHelperProvider().pushUserSettings()
            true
        } catch (e: Exception) {
            false
        }
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
                val currentOwnerUid = group?.ownerUid
                if (currentOwnerUid == uid) {
                    val nextOwner = memberIds.filter { it != uid }.random()
                    groupDoc.update(GroupInfo::ownerUid.name, nextOwner).await()
                }
                groupDoc.collection("members").document(uid).delete().await()
            }
            
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            val user = userSnapshot.toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            
            currentGroups.remove(groupId)
            val nextGroupId = if (settings.getCurrentGroupId() == groupId) {
                currentGroups.firstOrNull()
            } else {
                settings.getCurrentGroupId()
            }

            userRef.set(mapOf(User::groupIds.name to currentGroups), SetOptions.merge()).await()
            getUserDataColl().document(uid).update(com.jtool.android.core.models.user.UserData::currentGroupId.name, nextGroupId).await()
            
            clearLocalCache()
            settings.removeGroup(groupId)
            if (settings.getCurrentGroupId() == groupId) {
                settings.setCurrentGroupId(nextGroupId)
            }
            
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
        context.getSharedPreferences(PersistencePrefixEnum.SHOPPING.prefix, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences(PersistencePrefixEnum.NOTES.prefix, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences(PersistencePrefixEnum.LINKS.prefix, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences(PersistencePrefixEnum.CLEANING.prefix, Context.MODE_PRIVATE).edit().clear().apply()
    }

    suspend fun sendInvite(email: String, groupId: String): Boolean {
        val sanitizedEmail = email.lowercase().trim()
        return try {
            val groupSnapshot = getGroupsColl().document(groupId).get().await()
            val group = groupSnapshot.toObject(GroupInfo::class.java)
            val groupName = group?.name ?: "Unknown Group"
            val inviteData = mapOf(
                com.jtool.android.core.models.group.Invite::groupId.name to groupId,
                com.jtool.android.core.models.group.Invite::groupName.name to groupName,
                com.jtool.android.core.models.group.Invite::fromEmail.name to (auth.currentUser?.email ?: "Unknown"),
                com.jtool.android.core.models.group.Invite::timestamp.name to System.currentTimeMillis()
            )
            getInvitesColl().document(sanitizedEmail).collection("items").add(inviteData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun invitesFlow(): Flow<List<com.jtool.android.core.models.group.Invite>> = callbackFlow {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: run { close(); return@callbackFlow }
        val listener = getInvitesColl().document(email).collection("items").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(com.jtool.android.core.models.group.Invite::class.java)?.copy(inviteId = doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean): Boolean {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: return false
        return try {
            if (accept) {
                val success = joinGroup(groupId)
                if (success) {
                    getInvitesColl().document(email).collection("items").document(inviteId).delete().await()
                }
                success
            } else {
                getInvitesColl().document(email).collection("items").document(inviteId).delete().await()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun groupMembersFlow(groupId: String): Flow<List<String>> = callbackFlow {
        val listener = getGroupsColl().document(groupId).collection("members").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                trySend(snapshot.documents.map { it.id })
            }
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
        if (groupIds.isEmpty()) {
            trySend(emptyMap())
            awaitClose { }
            return@callbackFlow
        }
        val listener = getGroupsColl().whereIn(FieldPath.documentId(), groupIds.take(30)).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val map = snapshot.documents.associateBy({ it.id }, { doc ->
                    val group = doc.toObject(GroupInfo::class.java) ?: GroupInfo()
                    group.copy(id = doc.id)
                })
                trySend(map)
            }
        }
        awaitClose { listener.remove() }
    }
}
