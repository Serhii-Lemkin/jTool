package com.jtool.android.core.auth

import android.util.Log
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.jtool.android.R
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.models.common.enums.PersistencePrefixEnum
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    fun getSignInClient(): GoogleSignInClient {
        val webClientId = context.getString(R.string.default_web_client_id)
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
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            val name = account.displayName ?: firebaseUser?.displayName
            if (!name.isNullOrEmpty()) {
                val settings = SettingsManager(context)
                if (settings.getUserName().isEmpty() || settings.getUserName() == "User") {
                    settings.saveUserName(name)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("jTool", "Firebase Authentication Failed: ${e.message}", e)
            false
        }
    }

    fun signOut() {
        val settings = SettingsManager(context)
        settings.purgeAllUserData()
        auth.signOut()
        getSignInClient().signOut()
    }
}
