package com.jtool.android.core.sync.helpers

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.localization.Loc
import com.google.firebase.firestore.FieldValue
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.group.Invite
import com.jtool.android.core.models.group.enums.JoinGroupResultEnum
import com.jtool.android.core.settings.SettingsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GroupInviteHelper(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settings: SettingsManager,
    private val notificationHelper: NotificationHelper,
    private val groupOperationsHelperProvider: () -> GroupOperationsHelper
) {
    private fun getInvitesColl() = firestore.collection("invites")
    private fun getGroupsColl() = firestore.collection("groups")
    private fun getUserDataColl() = firestore.collection("user_data")

    suspend fun sendInvite(email: String, groupId: String): Boolean {
        val sanitizedEmail = email.lowercase().trim()
        return try {
            val groupName = try { 
                getGroupsColl().document(groupId).get().await().toObject(GroupInfo::class.java)?.name 
            } catch (e: Exception) { null } ?: settings.getGroups()[groupId]?.name ?: "New Group"
            
            getInvitesColl().document(sanitizedEmail).collection("items").document(groupId).set(
                mapOf(
                    Invite::groupId.name to groupId,
                    Invite::groupName.name to groupName,
                    Invite::fromEmail.name to (auth.currentUser?.email ?: "Unknown"),
                    "inviterUid" to (auth.currentUser?.uid ?: ""),
                    Invite::timestamp.name to System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Exception) { false }
    }

    fun invitesFlow(): Flow<List<Invite>> = callbackFlow {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: run { close(); return@callbackFlow }
        val listener = getInvitesColl().document(email).collection("items").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("jTool", "Error in invitesFlow: ${error.message}")
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toObject(Invite::class.java)?.copy(inviteId = it.id) })
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun respondToInvite(inviteId: String, groupId: String, accept: Boolean): Boolean {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: return false
        return try {
            if (accept) {
                val (result, _) = groupOperationsHelperProvider().joinGroup(groupId, isInviteAcceptance = true)
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
                        type = NotificationTypeEnum.GROUP_JOIN_DECLINED,
                        titleKey = "settings_invitationRejected",
                        messageKey = "settings_invitationRejectedMsg",
                        messageArgs = listOf(email, groupName)
                    )
                    notificationHelper.sendSystemNotification(inviterUid, notification)
                }
                true
            }
        } catch (e: Exception) { false }
    }
}
