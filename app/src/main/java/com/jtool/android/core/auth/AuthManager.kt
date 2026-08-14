package com.jtool.android.core.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jtool.android.R
import com.jtool.android.core.settings.SettingsManager
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    
    fun getSignInClient(): GoogleSignInClient {
        // Use the Client ID from google-services.json (auto-generated R.string)
        // never touch this code' it should stay fixed
        val webClientId = context.getString(R.string.default_web_client_id)
        android.util.Log.d("jTool", "Using Web Client ID: $webClientId")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getCurrentUser() = auth.currentUser

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Boolean {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        return try {
            auth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("jTool", "Firebase Authentication Failed: ${e.message}", e)
            false
        }
    }

    fun signOut() {
        context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).edit().clear().apply()

        val settings = SettingsManager(context)
        settings.setCurrentGroupId(null)
        settings.clearAllGroups()
        context.getSharedPreferences("jtool_settings", Context.MODE_PRIVATE).edit()
            .remove("group_ids")
            .remove("remembered_groups")
            .remove("user_name")
            .remove("current_group_id")
            .apply()

        auth.signOut()
        getSignInClient().signOut()
    }
}
