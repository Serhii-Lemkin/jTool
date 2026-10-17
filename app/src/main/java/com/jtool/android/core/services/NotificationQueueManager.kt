package com.jtool.android.core.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationState(
    val notificationCount: Int = 0,
    val onNotificationsClick: (() -> Unit)? = null
)

object NotificationQueueManager {
    private val _notificationState = MutableStateFlow(NotificationState())
    val notificationState: StateFlow<NotificationState> = _notificationState.asStateFlow()

    fun updateNotifications(count: Int, onClick: () -> Unit) {
        _notificationState.update {
            NotificationState(
                notificationCount = count,
                onNotificationsClick = onClick
            )
        }
    }
}
