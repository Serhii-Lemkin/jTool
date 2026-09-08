package com.jtool.android.features.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.models.common.UserNotification
import com.jtool.android.core.models.common.enums.NotificationTypeEnum
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
                        isGroupLevel = false
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
                        isGroupLevel = true
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
        }
    }
}
