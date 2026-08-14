package com.jtool.android.core.sync.helpers

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jtool.android.core.models.GroupInfo
import com.jtool.android.core.settings.SettingsManager
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

    fun currentGroupIdFlow(): Flow<String?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val listener = getUsersColl().document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val gid = snapshot.getString("currentGroupId")
                trySend(gid)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getUserGroups(): List<String> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snapshot = getUsersColl().document(uid).get().await()
        @Suppress("UNCHECKED_CAST")
        return (snapshot.get("groupIds") as? List<String>) ?: emptyList()
    }

    suspend fun createGroup(groupName: String): String? {
        val uid = auth.currentUser?.uid ?: return null
        
        val groupData = mapOf(
            "name" to groupName,
            "ownerUid" to uid,
            "createdAt" to System.currentTimeMillis()
        )
        
        return try {
            val groupDoc = getGroupsColl().add(groupData).await()
            val groupId = groupDoc.id
            groupDoc.collection("members").document(uid).set(mapOf("joinedAt" to System.currentTimeMillis())).await()
            groupId
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to create group", e)
            null
        }
    }

    suspend fun updateGroupName(groupId: String, newName: String): Boolean {
        return try {
            getGroupsColl().document(groupId).update("name", newName).await()
            val groups = settings.getGroups().toMutableMap()
            groups[groupId]?.let {
                groups[groupId] = it.copy(name = newName)
                settings.saveGroups(groups)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("GroupSync", "Failed to update group name", e)
            false
        }
    }

    suspend fun joinGroup(groupId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            clearLocalCache()
            getGroupsColl().document(groupId).collection("members").document(uid)
                .set(mapOf("joinedAt" to System.currentTimeMillis())).await()
            
            val groupSnapshot = getGroupsColl().document(groupId).get().await()
            val groupName = groupSnapshot.getString("name") ?: "New Group"
            val owner = groupSnapshot.getString("ownerUid")
            
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val currentGroups = (userSnapshot.get("groupIds") as? List<String>)?.toMutableList() ?: mutableListOf()
            
            if (!currentGroups.contains(groupId)) {
                currentGroups.add(groupId)
            }
            
            userRef.set(mapOf(
                "groupIds" to currentGroups,
                "currentGroupId" to groupId
            ), SetOptions.merge()).await()
            
            settings.addGroup(GroupInfo(id = groupId, name = groupName, owner = owner))
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
                val groupData = groupDoc.get().await()
                val ownerUid = groupData.getString("ownerUid")
                if (ownerUid == uid) {
                    val nextOwner = memberIds.filter { it != uid }.random()
                    groupDoc.update("ownerUid", nextOwner).await()
                }
                groupDoc.collection("members").document(uid).delete().await()
            }
            
            val userRef = getUsersColl().document(uid)
            val userSnapshot = userRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val currentGroups = (userSnapshot.get("groupIds") as? List<String>)?.toMutableList() ?: mutableListOf()
            
            currentGroups.remove(groupId)
            val nextGroupId = if (settings.getCurrentGroupId() == groupId) {
                currentGroups.firstOrNull()
            } else {
                settings.getCurrentGroupId()
            }

            userRef.set(mapOf(
                "groupIds" to currentGroups,
                "currentGroupId" to nextGroupId
            ), SetOptions.merge()).await()
            
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
        getUsersColl().document(uid).update("currentGroupId", groupId).await()
        settings.setCurrentGroupId(groupId)
        userAdminHelperProvider().pushUserSettings()
    }

    fun clearLocalCache() {
        context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).edit().clear().apply()
    }

    suspend fun sendInvite(email: String, groupId: String): Boolean {
        val sanitizedEmail = email.lowercase().trim()
        return try {
            val groupSnapshot = getGroupsColl().document(groupId).get().await()
            val groupName = groupSnapshot.getString("name") ?: "Unknown Group"
            val inviteData = mapOf(
                "groupId" to groupId,
                "groupName" to groupName,
                "fromEmail" to (auth.currentUser?.email ?: "Unknown"),
                "timestamp" to System.currentTimeMillis()
            )
            getInvitesColl().document(sanitizedEmail).collection("items").add(inviteData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun invitesFlow(): Flow<List<Map<String, String>>> = callbackFlow {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: run { close(); return@callbackFlow }
        val listener = getInvitesColl().document(email).collection("items").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.map { doc ->
                    val map = mutableMapOf<String, String>()
                    map["inviteId"] = doc.id
                    doc.data?.forEach { (k, v) -> map[k] = v.toString() }
                    map
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
                trySend(snapshot.get("groupIds") as? List<String> ?: emptyList())
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
                    GroupInfo(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        owner = doc.getString("ownerUid")
                    )
                })
                trySend(map)
            }
        }
        awaitClose { listener.remove() }
    }
}
