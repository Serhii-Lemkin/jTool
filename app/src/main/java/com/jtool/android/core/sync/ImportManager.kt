package com.jtool.android.core.sync

import android.net.Uri
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SharedTextContent(
    val text: String,
    val subject: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SharedUnsupportedContent(
    val mimeType: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingSupportDraft(
    val type: SupportTypeEnum = SupportTypeEnum.FEATURE,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ImportManager {
    private val _pendingSharedUris = MutableStateFlow<List<Uri>>(emptyList())
    val pendingSharedUris: StateFlow<List<Uri>> = _pendingSharedUris.asStateFlow()

    private val _pendingSharedText = MutableStateFlow<SharedTextContent?>(null)
    val pendingSharedText: StateFlow<SharedTextContent?> = _pendingSharedText.asStateFlow()

    private val _pendingUnsupportedShare = MutableStateFlow<SharedUnsupportedContent?>(null)
    val pendingUnsupportedShare: StateFlow<SharedUnsupportedContent?> = _pendingUnsupportedShare.asStateFlow()

    private val _pendingSupportDraft = MutableStateFlow<PendingSupportDraft?>(null)
    val pendingSupportDraft: StateFlow<PendingSupportDraft?> = _pendingSupportDraft.asStateFlow()

    fun setPendingSharedUris(uris: List<Uri>) {
        _pendingSharedUris.value = uris.filter { it != Uri.EMPTY }
    }

    fun clearPendingSharedUris() {
        _pendingSharedUris.value = emptyList()
    }

    fun setPendingSharedText(text: String, subject: String = "") {
        if (text.isNotBlank()) {
            _pendingSharedText.value = SharedTextContent(text = text.trim(), subject = subject.trim())
        }
    }

    fun clearPendingSharedText() {
        _pendingSharedText.value = null
    }

    fun setPendingUnsupportedShare(mimeType: String, details: String = "") {
        _pendingUnsupportedShare.value = SharedUnsupportedContent(
            mimeType = mimeType.ifBlank { "unknown" },
            details = details
        )
    }

    fun clearPendingUnsupportedShare() {
        _pendingUnsupportedShare.value = null
    }

    fun setPendingSupportDraft(text: String, type: SupportTypeEnum = SupportTypeEnum.FEATURE) {
        _pendingSupportDraft.value = PendingSupportDraft(type = type, text = text)
    }

    fun clearPendingSupportDraft() {
        _pendingSupportDraft.value = null
    }
}
