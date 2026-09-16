package com.jtool.android.core.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.jtool.android.BuildConfig
import java.security.MessageDigest

object DeepLinkUtils {
    /**
     * Checks if the app is verified to open BuildConfig.DEEP_LINK_HOST links.
     * On Android 12+, this checks the actual domain verification status.
     */
    fun isAppVerifiedForLinks(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.DOMAIN_VERIFICATION_SERVICE) as? DomainVerificationManager ?: return true
            val userState = try {
                manager.getDomainVerificationUserState(context.packageName)
            } catch (e: Exception) {
                null
            } ?: return true
            
            val hostState = userState.hostToStateMap[BuildConfig.DEEP_LINK_HOST]
            return hostState == DomainVerificationUserState.DOMAIN_STATE_SELECTED ||
                   hostState == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
        }
        // For older versions, we assume true or the disambiguation dialog handles it
        return true
    }

    /**
     * Returns the SHA256 signing fingerprint of the app.
     */
    fun getAppSignature(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return "Unknown"

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatures[0].toByteArray())
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Opens the system settings for the app's "Open by default" page.
     */
    fun openDeepLinkSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
