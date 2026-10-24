package com.jtool.android.core.models.vaults

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Keep
@IgnoreExtraProperties
@Serializable
data class Vault(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val driveFolderId: String = "",
    val driveFolderUrl: String = "",
    val docCount: Int = 0
)
