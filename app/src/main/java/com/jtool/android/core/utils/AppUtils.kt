package com.jtool.android.core.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

object AppUtils {
    fun requestNotificationPermission(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestCalendarPermission(activity: ComponentActivity, onResult: (Boolean) -> Unit) {
        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            onResult(results.values.all { it })
        }
        launcher.launch(arrayOf(
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.WRITE_CALENDAR
        ))
    }

    fun logAppSignature(context: Context) {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures != null) {
                for (signature in signatures) {
                    java.security.MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
                }
            }
        } catch (ignored: Exception) {
            // Error logging signature
        }
    }
}
