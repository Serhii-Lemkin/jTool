package com.jtool.android.features.support.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.utils.DeviceInfoProvider
import com.jtool.android.core.sync.helpers.CommentRepository
import com.jtool.android.core.components.comments.CommentController
import com.jtool.android.core.utils.SyncUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val commentController = CommentController(CommentRepository(), viewModelScope) { settings.getAppUserId() }
    val comments = commentController.comments

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
                                doc.toObject(SupportRequest::class.java)?.copy(id = doc.id)
                            }
                            
                            // Fetch comment counts in parallel
                            val listWithCounts = list.map { req ->
                                async {
                                    try {
                                        val countSnap = syncManager.getSupportColl().document(req.id).collection("comments").count()
                                            .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                                        req.apply { commentCount = countSnap.count.toInt() }
                                    } catch (e: Exception) {
                                        req
                                    }
                                }
                            }.awaitAll()

                            _uiState.update { state ->
                                val updatedList = listWithCounts.map { req ->
                                    val existing = state.requests.find { it.id == req.id }
                                    if (existing != null) {
                                        // Preserve UI states, but use fresh server data for core fields
                                        existing.copy(
                                            status = req.status,
                                            type = req.type,
                                            text = req.text,
                                            number = req.number,
                                            pendingStatus = if (req.status != existing.pendingStatus) existing.pendingStatus else null,
                                            pendingType = if (req.type != existing.pendingType) existing.pendingType else null
                                        ).apply { 
                                            // Real-time comment count from server aggregation
                                            commentCount = req.commentCount
                                        }
                                    } else req
                                }
                                
                                val selected = updatedList.find { it.id == state.selectedRequest?.id } ?: state.selectedRequest
                                state.copy(requests = updatedList, selectedRequest = selected, isLoading = false)
                            }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeCommentsState() {
        commentController.comments.onEach { updatedComments ->
            _uiState.update { state ->
                val selectedId = state.selectedRequest?.id
                val updatedRequests = state.requests.map { 
                    if (it.id == selectedId) {
                        it.copy(comments = updatedComments).apply { commentCount = updatedComments.size }
                    } else it
                }
                val updatedSelected = updatedRequests.find { it.id == selectedId } ?: state.selectedRequest
                state.copy(selectedRequest = updatedSelected, requests = updatedRequests)
            }
        }.launchIn(viewModelScope)
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
            is SupportEvent.UpdateStatus -> updateStatus(event.requestId, event.status)
            is SupportEvent.UpdateType -> updateType(event.requestId, event.type)
            
            is SupportEvent.AddComment -> commentController.addComment(event.text, event.parentId)
            is SupportEvent.EditComment -> commentController.editComment(event.commentId, event.newContent)
            is SupportEvent.DeleteComment -> commentController.deleteComment(event.commentId)
            
            is SupportEvent.DeleteRequest -> viewModelScope.launch { 
                syncManager.deleteSupportRequest(event.requestId)
                commentController.stop()
                _uiState.update { it.copy(currentSubScreen = SupportScreenType.HUB, selectedRequest = null) }
            }
            SupportEvent.ResetStatus -> _uiState.update { it.copy(isSuccess = false) }
            SupportEvent.Refresh -> {
                viewModelScope.launch {
                    SyncUtils.performRefresh(
                        onLoading = { loading -> 
                            _uiState.update { it.copy(isLoading = loading) }
                        }
                    ) {
                        _uiState.update { 
                            it.copy(
                                filterType = null,
                                searchQuery = "",
                                isSearchVisible = false,
                                showArchived = false,
                                pagingLimit = 15
                            )
                        }
                        
                        val isAdmin = _uiState.value.isAdmin
                        val query = if (isAdmin) {
                            syncManager.getSupportColl().orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        } else {
                            syncManager.getSupportColl().whereEqualTo("userId", settings.getAppUserId()).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        }
                        
                        val snapshot = query.get().await()
                        val list = snapshot.documents.mapNotNull { it.toObject(SupportRequest::class.java)?.copy(id = it.id) }
                        
                        // Fetch comment counts in parallel for the refreshed list
                        val listWithCounts = list.map { req ->
                            async {
                                try {
                                    val countSnap = syncManager.getSupportColl().document(req.id).collection("comments").count()
                                        .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                                    req.apply { commentCount = countSnap.count.toInt() }
                                } catch (e: Exception) {
                                    req
                                }
                            }
                        }.awaitAll()
                        
                        _uiState.update { it.copy(requests = listWithCounts) }
                    }
                }
            }
            is SupportEvent.Navigate -> {
                if (event.screen == SupportScreenType.HUB) commentController.stop()
                _uiState.update { 
                    it.copy(currentSubScreen = event.screen)
                }
            }
        }
    }

    private fun openRequest(request: SupportRequest) {
        _uiState.update { it.copy(selectedRequest = request, currentSubScreen = SupportScreenType.DETAIL) }
        commentController.observe(syncManager.getSupportColl().document(request.id).collection("comments"))
    }

    private fun updateStatus(requestId: String, status: SupportStatus) {
        _uiState.update { state ->
            val updatedRequests = state.requests.map { 
                if (it.id == requestId) it.copy(pendingStatus = status) else it
            }
            val updatedSelected = state.selectedRequest?.let { 
                if (it.id == requestId) it.copy(pendingStatus = status) else it
            }
            state.copy(requests = updatedRequests, selectedRequest = updatedSelected)
        }
        
        viewModelScope.launch {
            try {
                syncManager.updateSupportStatus(requestId, status)
            } catch (e: Exception) {
                // Revert ghost on error
                _uiState.update { state ->
                    val updatedRequests = state.requests.map { 
                        if (it.id == requestId) it.copy(pendingStatus = null) else it
                    }
                    val updatedSelected = state.selectedRequest?.let { 
                        if (it.id == requestId) it.copy(pendingStatus = null) else it
                    }
                    state.copy(requests = updatedRequests, selectedRequest = updatedSelected)
                }
            }
        }
    }

    private fun updateType(requestId: String, type: SupportType) {
        _uiState.update { state ->
            val updatedRequests = state.requests.map { 
                if (it.id == requestId) it.copy(pendingType = type) else it
            }
            val updatedSelected = state.selectedRequest?.let { 
                if (it.id == requestId) it.copy(pendingType = type) else it
            }
            state.copy(requests = updatedRequests, selectedRequest = updatedSelected)
        }
        
        viewModelScope.launch {
            try {
                syncManager.updateSupportType(requestId, type)
            } catch (e: Exception) {
                // Revert ghost on error
                _uiState.update { state ->
                    val updatedRequests = state.requests.map { 
                        if (it.id == requestId) it.copy(pendingType = null) else it
                    }
                    val updatedSelected = state.selectedRequest?.let { 
                        if (it.id == requestId) it.copy(pendingType = null) else it
                    }
                    state.copy(requests = updatedRequests, selectedRequest = updatedSelected)
                }
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
        commentController.stop()
    }
}
