package com.example.jtool

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    
    fun getSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
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
            false
        }
    }

    fun signOut() {
        // Clear local data on logout
        context.getSharedPreferences("jtool_shopping", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_notes", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_links", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("jtool_cleaning", Context.MODE_PRIVATE).edit().clear().apply()
        
        // Also clear active group but maybe keep the list?
        // Let's clear everything related to the previous user session
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
