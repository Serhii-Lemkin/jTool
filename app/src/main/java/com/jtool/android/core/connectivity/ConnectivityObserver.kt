package com.jtool.android.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ConnectivityObserver(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val groupSyncManager: GroupSyncManager,
    private val scope: CoroutineScope
) {
    fun start() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (settingsManager.isSyncPending()) {
                        scope.launch {
                            val success = groupSyncManager.pushGroupData()
                            settingsManager.setSyncPending(!success)
                        }
                    }
                }
            }
        )
    }
}
