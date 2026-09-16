package com.jtool.android.features.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.models.common.UserNotification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminViewModel(
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        dataManager.userNamesFlow()
            .onEach { names -> _uiState.update { it.copy(userNames = names) } }
            .launchIn(viewModelScope)

        dataManager.allUsersFlow()
            .onEach { users -> _uiState.update { it.copy(users = users) } }
            .launchIn(viewModelScope)

        dataManager.allGroupsFlow()
            .onEach { groups -> _uiState.update { it.copy(groups = groups) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AdminEvent) {
        when (event) {
            is AdminEvent.Navigate -> {
                _uiState.update { it.copy(currentSubScreen = event.subScreen) }
            }
            is AdminEvent.UpdateUserSearchQuery -> {
                _uiState.update { it.copy(userSearchQuery = event.query) }
            }
            AdminEvent.ApplyUserSearch -> {
                _uiState.update { it.copy(appliedUserSearchQuery = it.userSearchQuery) }
            }
            is AdminEvent.SetUserAdminStatus -> {
                viewModelScope.launch {
                    dataManager.setUserAdminStatus(event.uid, event.isAdmin)
                }
            }
            is AdminEvent.RequestDeleteUser -> {
                if (event.step == 1) {
                    _uiState.update { it.copy(showUserDeleteConfirm1 = event.uid) }
                } else {
                    _uiState.update { it.copy(showUserDeleteConfirm1 = null, showUserDeleteConfirm2 = event.uid) }
                }
            }
            is AdminEvent.ConfirmDeleteUser -> {
                viewModelScope.launch {
                    dataManager.deleteUserPermanently(event.uid)
                    _uiState.update { it.copy(showUserDeleteConfirm2 = null) }
                }
            }
            is AdminEvent.UpdateGroupSearchQuery -> {
                _uiState.update { it.copy(groupSearchQuery = event.query) }
            }
            AdminEvent.ApplyGroupSearch -> {
                _uiState.update { it.copy(appliedGroupSearchQuery = it.groupSearchQuery) }
            }
            is AdminEvent.RequestDeleteGroup -> {
                if (event.step == 1) {
                    _uiState.update { it.copy(showGroupDeleteConfirm1 = event.gid) }
                } else {
                    _uiState.update { it.copy(showGroupDeleteConfirm1 = null, showGroupDeleteConfirm2 = event.gid) }
                }
            }
            is AdminEvent.ConfirmDeleteGroup -> {
                viewModelScope.launch {
                    dataManager.deleteGroupPermanently(event.gid)
                    _uiState.update { it.copy(showGroupDeleteConfirm2 = null) }
                }
            }
            is AdminEvent.UpdateNotifUserSearch -> _uiState.update { it.copy(notifUserSearchQuery = event.query) }
            AdminEvent.ApplyNotifUserSearch -> _uiState.update { it.copy(appliedNotifUserSearch = it.notifUserSearchQuery) }
            is AdminEvent.UpdateNotifGroupSearch -> _uiState.update { it.copy(notifGroupSearchQuery = event.query) }
            AdminEvent.ApplyNotifGroupSearch -> _uiState.update { it.copy(appliedNotifGroupSearch = it.notifGroupSearchQuery) }
            is AdminEvent.SendPersonalNotification -> {
                viewModelScope.launch {
                    val notification = UserNotification(
                        id = UUID.randomUUID().toString(),
                        title = event.title,
                        message = event.message,
                        timestamp = System.currentTimeMillis(),
                        type = event.type,
                        isGroupLevel = false,
                        titleKey = if (event.title.startsWith("key:")) event.title.removePrefix("key:") else null,
                        messageKey = if (event.message.startsWith("key:")) event.message.removePrefix("key:") else null
                    )
                    FirebaseFirestore.getInstance()
                        .collection("user_data")
                        .document(event.uid)
                        .update("notifications", FieldValue.arrayUnion(notification))
                        .await()
                }
            }
            is AdminEvent.SendGroupNotification -> {
                viewModelScope.launch {
                    val notification = UserNotification(
                        id = UUID.randomUUID().toString(),
                        groupId = event.gid,
                        title = event.title,
                        message = event.message,
                        timestamp = System.currentTimeMillis(),
                        type = event.type,
                        isGroupLevel = true,
                        titleKey = if (event.title.startsWith("key:")) event.title.removePrefix("key:") else null,
                        messageKey = if (event.message.startsWith("key:")) event.message.removePrefix("key:") else null
                    )
                    FirebaseFirestore.getInstance()
                        .collection("groups")
                        .document(event.gid)
                        .collection("group_notifications")
                        .document(notification.id)
                        .set(notification)
                        .await()
                }
            }
            AdminEvent.Refresh -> {
                _uiState.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    delay(500)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
            AdminEvent.CleanupCalendarData -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        val db = FirebaseFirestore.getInstance()
                        
                        // 1. Clear calendarConfig for all users
                        val userDataColl = db.collection("user_data")
                        val users = userDataColl.get().await()
                        users.documents.forEach { doc ->
                            userDataColl.document(doc.id).update("calendarConfig", FieldValue.delete()).await()
                        }

                        // 2. Delete all documents in calendar_events collection
                        val eventsColl = db.collection("calendar_events")
                        val events = eventsColl.get().await()
                        db.runBatch { batch ->
                            events.documents.forEach { batch.delete(it.reference) }
                        }.await()

                        // 3. Clear calendarEmail from public_profiles
                        val profilesColl = db.collection("public_profiles")
                        val profiles = profilesColl.get().await()
                        profiles.documents.forEach { doc ->
                            profilesColl.document(doc.id).update("calendarEmail", FieldValue.delete()).await()
                        }

                        // 4. Clear calendarEmail from group members
                        val groupsColl = db.collection("groups")
                        val groups = groupsColl.get().await()
                        groups.documents.forEach { gDoc ->
                            val members = groupsColl.document(gDoc.id).collection("members").get().await()
                            members.documents.forEach { mDoc ->
                                groupsColl.document(gDoc.id).collection("members").document(mDoc.id)
                                    .update("calendarEmail", FieldValue.delete()).await()
                            }
                        }

                        Log.d("AdminVM", "Calendar data cleanup completed")
                    } catch (e: Exception) {
                        Log.e("AdminVM", "Cleanup failed", e)
                    } finally {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }
}
