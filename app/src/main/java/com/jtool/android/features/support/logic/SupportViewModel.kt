package com.jtool.android.features.support.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.utils.DeviceInfoProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SupportViewModel(
    private val syncManager: GroupSyncManager,
    private val settings: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    private var requestsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var syncJob: kotlinx.coroutines.Job? = null

    init {
        loadConfig()
        observeAdminStatus()
        observeRequests()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            val options = syncManager.getAppOptions()
            options?.let {
                _uiState.update { state ->
                    state.copy(
                        maxRequests = (it["max_requests"] as? Long)?.toInt() ?: 2,
                        privacyUrl = it["privacy"] as? String ?: ""
                    )
                }
            }
        }
    }

    private fun observeAdminStatus() {
        syncManager.isAdminFlow()
            .onEach { isAdmin -> _uiState.update { it.copy(isAdmin = isAdmin) } }
            .launchIn(viewModelScope)
    }

    private fun observeRequests() {
        uiState.map { it.isAdmin }
            .distinctUntilChanged()
            .onEach { isAdmin ->
                requestsListener?.remove()
                _uiState.update { it.copy(isLoading = true) }
                val query = if (isAdmin) {
                    syncManager.getSupportColl().orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                } else {
                    syncManager.getSupportColl().whereEqualTo("userId", settings.getAppUserId()).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                }

                requestsListener = query.addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        syncJob?.cancel()
                        syncJob = viewModelScope.launch {
                            val list = snapshot.documents.mapNotNull { doc ->
                                val req = doc.toObject(SupportRequest::class.java)?.copy(id = doc.id)
                                if (req != null) {
                                    val cSnap = doc.reference.collection("comments").get().await()
                                    val allComments = cSnap.documents.mapNotNull { cDoc ->
                                        cDoc.toObject(com.jtool.android.core.models.Comment::class.java)?.copy(id = cDoc.id)
                                    }
                                    req.copy(comments = buildCommentTree(allComments))
                                } else null
                            }
                            
                            _uiState.update { state ->
                                val selected = list.find { it.id == state.selectedRequest?.id } ?: state.selectedRequest
                                state.copy(requests = list, selectedRequest = selected, isLoading = false)
                            }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun buildCommentTree(allComments: List<com.jtool.android.core.models.Comment>): List<com.jtool.android.core.models.Comment> {
        val roots = mutableListOf<com.jtool.android.core.models.Comment>()
        val children = mutableMapOf<String, MutableList<com.jtool.android.core.models.Comment>>()

        allComments.forEach { comment ->
            val pId = comment.parentId
            if (pId == null) {
                roots.add(comment)
            } else {
                children.getOrPut(pId) { mutableListOf() }.add(comment)
            }
        }

        fun assemble(comment: com.jtool.android.core.models.Comment): com.jtool.android.core.models.Comment {
            val replies = children[comment.id]
                ?.map { assemble(it) }
                ?.sortedBy { it.timestamp } ?: emptyList()
            return comment.copy(replies = replies)
        }

        return roots.map { assemble(it) }.sortedByDescending { it.timestamp }
    }

    fun onEvent(event: SupportEvent) {
        when (event) {
            is SupportEvent.SetFilterType -> _uiState.update { it.copy(filterType = event.type, pagingLimit = 15) }
            is SupportEvent.ToggleArchived -> _uiState.update { it.copy(showArchived = event.show, pagingLimit = 15) }
            is SupportEvent.ToggleSearch -> _uiState.update { it.copy(isSearchVisible = !it.isSearchVisible, searchQuery = "") }
            is SupportEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.query, pagingLimit = 15) }
            SupportEvent.LoadMore -> _uiState.update { it.copy(pagingLimit = it.pagingLimit + 15) }
            is SupportEvent.UpdateRequestText -> _uiState.update { it.copy(requestText = event.text) }
            is SupportEvent.SubmitRequest -> submitRequest(event.type, event.context)
            is SupportEvent.OpenRequest -> openRequest(event.request)
            is SupportEvent.UpdateStatus -> viewModelScope.launch { syncManager.updateSupportStatus(event.requestId, event.status) }
            is SupportEvent.UpdateType -> viewModelScope.launch { syncManager.updateSupportType(event.requestId, event.type) }
            
            is SupportEvent.AddComment -> addComment(event.requestId, event.text, event.parentId)
            is SupportEvent.EditComment -> editComment(event.requestId, event.commentId, event.newContent)
            is SupportEvent.DeleteComment -> deleteComment(event.requestId, event.commentId)
            
            is SupportEvent.DeleteRequest -> viewModelScope.launch { 
                syncManager.deleteSupportRequest(event.requestId)
                _uiState.update { it.copy(currentSubScreen = SupportScreenType.HUB, selectedRequest = null) }
            }
            SupportEvent.ResetStatus -> _uiState.update { it.copy(isSuccess = false) }
            SupportEvent.Refresh -> _uiState.update { 
                it.copy(
                    filterType = null,
                    searchQuery = "",
                    isSearchVisible = false,
                    showArchived = false,
                    pagingLimit = 15,
                    isLoading = true
                )
            }
            is SupportEvent.Navigate -> {
                _uiState.update { 
                    val newState = it.copy(currentSubScreen = event.screen)
                    if (event.screen == SupportScreenType.HUB) {
                        newState.copy(
                            filterType = null,
                            searchQuery = "",
                            isSearchVisible = false,
                            showArchived = false,
                            pagingLimit = 15,
                            isLoading = true
                        )
                    } else newState
                }
            }
        }
    }

    private fun openRequest(request: SupportRequest) {
        _uiState.update { it.copy(selectedRequest = request, currentSubScreen = SupportScreenType.DETAIL) }
    }

    private fun addComment(requestId: String, content: String, parentId: String?) {
        val currentUserId = settings.getAppUserId()
        val tempId = "temp_${UUID.randomUUID()}"
        val ghostComment = com.jtool.android.core.models.Comment(
            id = tempId,
            content = content,
            userId = currentUserId,
            timestamp = System.currentTimeMillis(),
            parentId = parentId,
            isPending = true
        )

        // Optimistic UI Update for detail view
        _uiState.update { state ->
            val updatedSelected = state.selectedRequest?.let { req ->
                if (req.id == requestId) {
                    val updatedComments = if (parentId == null) {
                        listOf(ghostComment) + req.comments
                    } else {
                        fun updateReplies(list: List<com.jtool.android.core.models.Comment>): List<com.jtool.android.core.models.Comment> = list.map { c ->
                            if (c.id == parentId) {
                                c.copy(replies = listOf(ghostComment) + c.replies)
                            } else {
                                c.copy(replies = updateReplies(c.replies))
                            }
                        }
                        updateReplies(req.comments)
                    }
                    req.copy(comments = updatedComments)
                } else req
            }
            
            state.copy(selectedRequest = updatedSelected)
        }

        viewModelScope.launch {
            try {
                val realComment = ghostComment.copy(id = UUID.randomUUID().toString(), isPending = false)
                syncManager.addSupportComment(requestId, realComment)
            } catch (e: Exception) {
                android.util.Log.e("SupportVM", "Failed to add comment", e)
            }
        }
    }

    private fun editComment(requestId: String, commentId: String, newContent: String) {
        // Optimistic update for edit
        _uiState.update { state ->
            val updatedSelected = state.selectedRequest?.let { req ->
                if (req.id == requestId) {
                    fun updateContent(list: List<com.jtool.android.core.models.Comment>): List<com.jtool.android.core.models.Comment> = list.map { c ->
                        if (c.id == commentId) {
                            c.copy(content = newContent, isEdited = true)
                        } else {
                            c.copy(replies = updateContent(c.replies))
                        }
                    }
                    req.copy(comments = updateContent(req.comments))
                } else req
            }
            state.copy(selectedRequest = updatedSelected)
        }

        viewModelScope.launch {
            try {
                syncManager.editSupportComment(requestId, commentId, newContent)
            } catch (e: Exception) {
                android.util.Log.e("SupportVM", "Failed to edit comment", e)
            }
        }
    }

    private fun deleteComment(requestId: String, commentId: String) {
        // Optimistic update for delete
        _uiState.update { state ->
            val updatedSelected = state.selectedRequest?.let { req ->
                if (req.id == requestId) {
                    fun removeComment(list: List<com.jtool.android.core.models.Comment>): List<com.jtool.android.core.models.Comment> = list.filter { it.id != commentId }.map { 
                        it.copy(replies = removeComment(it.replies))
                    }
                    req.copy(comments = removeComment(req.comments))
                } else req
            }
            
            state.copy(selectedRequest = updatedSelected)
        }

        viewModelScope.launch {
            try {
                syncManager.deleteSupportComment(requestId, commentId)
            } catch (e: Exception) {
                android.util.Log.e("SupportVM", "Failed to delete comment", e)
            }
        }
    }

    private fun submitRequest(type: SupportType, context: android.content.Context) {
        val text = _uiState.value.requestText.trim()
        if (text.isEmpty()) return

        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            try {
                val request = SupportRequest(
                    type = type,
                    text = text,
                    userId = settings.getAppUserId(),
                    userName = settings.getUserName(),
                    timestamp = System.currentTimeMillis(),
                    deviceInfo = DeviceInfoProvider.getDeviceInfo(context),
                    status = SupportStatus.OPENED
                )
                
                val success = syncManager.submitSupportRequest(request, settings.getUserId())
                if (success) {
                    _uiState.update { it.copy(
                        isSending = false, 
                        isSuccess = true, 
                        requestText = "", 
                        currentSubScreen = SupportScreenType.HUB,
                        filterType = null,
                        searchQuery = "",
                        isSearchVisible = false,
                        showArchived = false,
                        pagingLimit = 15
                    ) }
                } else {
                    _uiState.update { it.copy(isSending = false, error = "Submission failed") }
                }
            } catch (e: Exception) {
                val errorMessage = if (e.message == "LIMIT_REACHED") {
                    Loc.support_daily_limit.format(uiState.value.maxRequests, uiState.value.maxRequests)
                } else {
                    "Error: ${e.localizedMessage ?: "Unknown error"}"
                }
                _uiState.update { it.copy(isSending = false, error = errorMessage) }
            }
        }
    }

    fun getAppUserId(): String = settings.getAppUserId()
    fun getAuthUserId(): String = settings.getUserId()

    override fun onCleared() {
        super.onCleared()
        requestsListener?.remove()
    }
}
