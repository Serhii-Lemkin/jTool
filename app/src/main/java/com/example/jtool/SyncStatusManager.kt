package com.example.jtool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR
}

object SyncStatusManager {
    var status by mutableStateOf(SyncStatus.IDLE)
        private set

    private var resetJob: kotlinx.coroutines.Job? = null

    fun startSync() {
        resetJob?.cancel()
        status = SyncStatus.SYNCING
    }

    fun endSync(success: Boolean) {
        status = if (success) SyncStatus.SUCCESS else SyncStatus.ERROR
        
        resetJob?.cancel()
        resetJob = CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            status = SyncStatus.IDLE
        }
    }
}
