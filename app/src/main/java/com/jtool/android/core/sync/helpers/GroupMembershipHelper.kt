package com.jtool.android.core.sync.helpers

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.jtool.android.core.models.group.JoinRequest
import com.jtool.android.core.models.group.MemberInfo
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.user.User
import com.jtool.android.core.models.user.UserData
import com.jtool.android.core.settings.SettingsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GroupMembershipHelper(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsManager,
    private val notificationHelper: NotificationHelper
) {
    private val emailCache = mutableMapOf<String, Pair<Long, List<String>>>()
    private val CACHE_DURATION = 5 * 60 * 1000L

    private fun getGroupsColl() = firestore.collection("groups")
    private fun getUsersColl() = firestore.collection("users")

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

    fun groupMemberEmailsFlow(groupId: String): Flow<List<String>> = callbackFlow {
        val listener = getGroupsColl().document(groupId).collection("members").addSnapshotListener { snapshot, _ -> 
            if (snapshot != null) trySend(snapshot.documents.mapNotNull { it.getString("calendarEmail") }) 
        }
        awaitClose { listener.remove() }
    }

    suspend fun getGroupMemberEmails(groupId: String): List<String> {
        val now = System.currentTimeMillis()
        emailCache[groupId]?.let { (timestamp, emails) ->
            if (now - timestamp < CACHE_DURATION) return emails
        }

        return try {
            val memberIds = getGroupsColl().document(groupId).collection("members")
                .get().await().documents.map { it.id }
            
            if (memberIds.isEmpty()) return emptyList()
            
            val emails = mutableListOf<String>()
            memberIds.chunked(10).forEach { chunk ->
                val profiles = firestore.collection("public_profiles")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get().await()
                
                profiles.documents.forEach { doc ->
                    val calEmail = doc.getString("calendarEmail")
                    val accEmail = doc.getString("email")
                    if (!calEmail.isNullOrEmpty()) emails.add(calEmail)
                    else if (!accEmail.isNullOrEmpty()) emails.add(accEmail)
                }

                val remainingIds = chunk.filter { id -> 
                    profiles.documents.none { it.id == id && !it.getString("calendarEmail").isNullOrEmpty() }
                }
                
                if (remainingIds.isNotEmpty()) {
                    val users = getUsersColl().whereIn(FieldPath.documentId(), remainingIds).get().await()
                    emails.addAll(users.documents.mapNotNull { it.getString("email") })
                }
            }
            val result = emails.distinct()
            emailCache[groupId] = now to result
            result
        } catch (e: Exception) { 
            Log.e("jTool", "Error fetching member emails: ${e.message}")
            emptyList() 
        }
    }

    suspend fun sendJoinRequest(groupId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val groupData = getGroupsColl().document(groupId).get().await().toObject(GroupInfo::class.java)
            if (groupData == null || groupData.isPrivate || groupData.isInviteOnly) return false
            val request = JoinRequest(
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

    fun joinRequestsFlow(groupId: String): Flow<List<JoinRequest>> = callbackFlow {
        val listener = getGroupsColl().document(groupId).collection("join_requests").addSnapshotListener { snapshot, error -> 
            if (error != null) {
                Log.e("jTool", "Error in joinRequestsFlow for $groupId: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject(JoinRequest::class.java) })
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun acceptJoinRequest(request: JoinRequest): Boolean {
        return try {
            val profileSnap = firestore.collection("public_profiles").document(request.userId).get().await()
            val calEmail = profileSnap.getString("calendarEmail")
            
            val groupRef = getGroupsColl().document(request.groupId)
            groupRef.collection("members").document(request.userId).set(MemberInfo(joinedAt = System.currentTimeMillis(), calendarEmail = calEmail)).await()
            
            val userRef = getUsersColl().document(request.userId)
            val user = userRef.get().await().toObject(User::class.java)
            val currentGroups = user?.groupIds?.toMutableList() ?: mutableListOf()
            if (!currentGroups.contains(request.groupId)) currentGroups.add(request.groupId)
            userRef.update(User::groupIds.name, currentGroups).await()
            
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
                excludeUid = request.userId,
                titleKey = "settings_memberJoined",
                messageKey = "settings_memberJoinedMsg",
                messageArgs = listOf(request.userName)
            )
            notificationHelper.sendGroupLevelNotification(request.groupId, groupNotif)
            
            val personalNotif = UserNotification(
                id = UUID.randomUUID().toString(),
                title = Loc.Settings.requestApproved, 
                message = Loc.Settings.addedToGroup.format(groupName), 
                timestamp = System.currentTimeMillis(), 
                type = NotificationTypeEnum.GROUP_JOIN_ACCEPTED,
                titleKey = "settings_requestApproved",
                messageKey = "settings_addedToGroup",
                messageArgs = listOf(groupName)
            )
            notificationHelper.sendSystemNotification(request.userId, personalNotif)
            true
        } catch (e: Exception) { false }
    }

    suspend fun declineJoinRequest(request: JoinRequest): Boolean {
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
                type = NotificationTypeEnum.GROUP_JOIN_DECLINED,
                titleKey = "settings_requestDeclined",
                messageKey = "settings_requestToJoinDeclined",
                messageArgs = listOf(groupName)
            )
            notificationHelper.sendSystemNotification(request.userId, notification)
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateUserCalendarEmailInGroups(email: String) {
        val uid = auth.currentUser?.uid ?: return
        val userSnapshot = getUsersColl().document(uid).get().await()
        val user = userSnapshot.toObject(User::class.java)
        val groupIds = user?.groupIds ?: emptyList()
        
        firestore.runBatch { batch ->
            groupIds.forEach { gid ->
                val memberRef = getGroupsColl().document(gid).collection("members").document(uid)
                batch.update(memberRef, "calendarEmail", email)
            }
        }.await()
    }
}
