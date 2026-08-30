package com.jtool.android.core.utils

import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis

object SyncUtils {
    suspend fun performRefresh(
        minDelay: Long = 800L,
        onLoading: (Boolean) -> Unit,
        block: suspend () -> Unit
    ) {
        onLoading(true)
        val elapsed = measureTimeMillis {
            try {
                block()
            } catch (e: Exception) {
                android.util.Log.e("SyncUtils", "Refresh failed", e)
            }
        }
        if (elapsed < minDelay) {
            delay(minDelay - elapsed)
        }
        onLoading(false)
    }
}
