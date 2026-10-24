package com.jtool.android.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WebUtils {
    fun openAppPage(
        context: Context,
        settings: SettingsManager,
        dataManager: DataManager? = null,
        appPath: String = ""
    ) {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            var configWebsiteUrl: String? = null
            if (dataManager != null) {
                try {
                    val config = dataManager.getAppOptions()
                    val fetchedUrl = config?.website?.ifBlank { null } ?: config?.privacy?.ifBlank { null }
                    if (!fetchedUrl.isNullOrBlank()) {
                        configWebsiteUrl = fetchedUrl
                        settings.setCachedWebsiteUrl(fetchedUrl)
                    }
                } catch (_: Exception) {}
            }

            if (configWebsiteUrl.isNullOrBlank()) {
                configWebsiteUrl = settings.getCachedWebsiteUrl()
            }

            var baseUrl = configWebsiteUrl ?: ""
            if (baseUrl.contains("/privacy")) {
                baseUrl = baseUrl.substringBefore("/privacy")
            }
            baseUrl = baseUrl.trimEnd('/')

            if (baseUrl.isBlank()) {
                return@launch
            }

            val lang = settings.getLanguage()
            val theme = if (settings.isDarkMode()) "dark" else "light"
            val path = when {
                appPath.isBlank() || appPath == "/" -> ""
                appPath.startsWith("/") -> appPath
                else -> "/$appPath"
            }

            val fullUrl = "$baseUrl$path?lang=$lang&theme=$theme"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
