package com.jtool.android.core.sync

import com.jtool.android.core.models.system.enums.SyncStatusEnum

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



object SyncStatusManager {
    var status by mutableStateOf(SyncStatusEnum.IDLE)
        private set

    private var resetJob: kotlinx.coroutines.Job? = null

    fun startSync() {
        resetJob?.cancel()
        status = SyncStatusEnum.SYNCING
    }

    fun endSync(success: Boolean) {
        status = if (success) SyncStatusEnum.SUCCESS else SyncStatusEnum.ERROR
        
        resetJob?.cancel()
        resetJob = CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            status = SyncStatusEnum.IDLE
        }
    }
}
