package com.jtool.android.core.utils

import android.content.Context
import android.os.Build

object DeviceInfoProvider {
    fun getDeviceInfo(context: Context): Map<String, String> {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return mapOf(
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to Build.VERSION.RELEASE,
            "sdkVersion" to Build.VERSION.SDK_INT.toString(),
            "appVersion" to (packageInfo.versionName ?: "unknown"),
            "versionCode" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        )
    }
}
