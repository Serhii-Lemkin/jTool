package com.jtool.android.core.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Keep
@IgnoreExtraProperties
@Serializable
data class NotesConfig(
    val previewMaxLines: Int = 2
)
