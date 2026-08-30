package com.jtool.android.core.models.feature.notes

import androidx.annotation.Keep
import com.jtool.android.core.models.common.Comment
import com.jtool.android.core.models.feature.notes.enums.NoteTypeEnum
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class NoteItem(
    var id: String = "",
    val title: String = "",
    val content: String = "",
    val type: NoteTypeEnum = NoteTypeEnum.TEXT,
    val checklist: List<ChecklistItem> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "",
    
    @get:com.google.firebase.firestore.Exclude
    @set:com.google.firebase.firestore.Exclude
    var checklistCount: Int = 0,
    
    @get:com.google.firebase.firestore.Exclude
    @set:com.google.firebase.firestore.Exclude
    var commentCount: Int = 0
)
