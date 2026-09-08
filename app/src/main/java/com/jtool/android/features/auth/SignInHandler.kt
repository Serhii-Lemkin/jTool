package com.jtool.android.features.auth

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.jtool.android.core.auth.AuthManager
import com.jtool.android.core.sync.DataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SignInHandler(
    private val activity: ComponentActivity,
    private val authManager: AuthManager,
    private val dataManager: DataManager,
    private val scope: CoroutineScope
) {
    var onLoadingChanged: (Boolean) -> Unit = {}
    var onError: (String) -> Unit = {}

    private val signInLauncher: ActivityResultLauncher<android.content.Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("jTool", "Sign-in result received. ResultCode: ${result.resultCode}, HasData: ${result.data != null}")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    onLoadingChanged(true)
                    scope.launch {
                        val success = authManager.signInWithGoogle(account)
                        if (success) {
                            dataManager.fetchUserSettings()
                            dataManager.pushUserSettings()
                        } else {
                            onError("Firebase Auth Failed")
                        }
                        onLoadingChanged(false)
                    }
                }
            } catch (e: Exception) {
                onLoadingChanged(false)
                Log.e("jTool", "Sign in failed", e)
                val msg = if (e is ApiException) {
                    val statusCodeString = CommonStatusCodes.getStatusCodeString(e.statusCode)
                    "Error Code: ${e.statusCode} ($statusCodeString)\nMessage: ${e.status.statusMessage ?: "No message"}"
                } else {
                    e.message ?: "Unknown Error"
                }
                onError("Sign-in Error: $msg")
            }
        }

    fun launchSignIn() {
        signInLauncher.launch(authManager.getSignInClient().signInIntent)
    }
}
