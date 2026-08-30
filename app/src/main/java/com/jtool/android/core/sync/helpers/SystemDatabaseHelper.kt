package com.jtool.android.core.sync.helpers

import com.google.firebase.firestore.FirebaseFirestore
import com.jtool.android.core.models.system.RequestedLanguage
import kotlinx.coroutines.tasks.await

class SystemDatabaseHelper(private val firestore: FirebaseFirestore) {
    
    suspend fun logRequestedLanguage(name: String, code: String) {
        try {
            val requestedLang = RequestedLanguage(
                name = name,
                code = code
            )
            firestore.collection("requested_languages").add(requestedLang).await()
        } catch (_: Exception) {}
    }
}
