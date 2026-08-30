package com.jtool.android.features.support.logic

import androidx.lifecycle.viewModelScope
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.BaseSyncViewModel
import com.jtool.android.core.utils.DeviceInfoProvider
import com.jtool.android.core.sync.helpers.CommentRepository
import com.jtool.android.core.components.comments.CommentController
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportScreenTypeEnum
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SupportViewModel(
    private val dataManager: DataManager,
    private val settings: SettingsManager
) : BaseSyncViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    private val commentController = CommentController(CommentRepository(), viewModelScope) { settings.getAppUserId() }
    val comments = commentController.comments

    init {
        loadConfig()
        observeAdminStatus()
        observeRequests()
        
        isLoading.onEach { loading -> _uiState.update { it.copy(isLoading = loading) } }.launchIn(viewModelScope)
    }

    private fun loadConfig() {
        viewModelScope.launch {
            val options = dataManager.getAppOptions()
            options?.let { config ->
                _uiState.update { state ->
                    state.copy(
                        maxRequests = config.max_requests.toInt(),
                        privacyUrl = config.privacy
                    )
                }
            }
        }
    }

    private fun observeAdminStatus() {
        dataManager.isAdminFlow()
            .onEach { isAdmin -> _uiState.update { it.copy(isAdmin = isAdmin) } }
            .launchIn(viewModelScope)
    }

    private fun observeRequests() {
        uiState.map { it.isAdmin }
            .distinctUntilChanged()
            .onEach { isAdmin ->
                clearListeners()
                _uiState.update { it.copy(isLoading = true) }
                val query = if (isAdmin) {
                    dataManager.getSupportColl().orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                } else {
                    dataManager.getSupportColl().whereEqualTo("userId", settings.getAppUserId()).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                }

                val listener = query.addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        cancelJobs()
                        val job = viewModelScope.launch {
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(SupportRequest::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("SupportVM", "Error deserializing request ${doc.id}", e)
                                    null
                                }
                            }
                            
                            _uiState.update { state ->
                                val updatedList = list.map { req ->
                                    val existing = state.requests.find { it.id == req.id }
                                    if (existing != null) {
                                        existing.copy(
                                            status = req.status,
                                            type = req.type,
                                            text = req.text,
                                            number = req.number,
                                            pendingStatus = if (req.status != existing.pendingStatus) existing.pendingStatus else null,
                                            pendingType = if (req.type != existing.pendingType) existing.pendingType else null
                                        ).apply { 
                                            commentCount = existing.commentCount
                                        }
                                    } else req
                                }
                                
                                val selected = updatedList.find { it.id == state.selectedRequest?.id } ?: state.selectedRequest
                                state.copy(requests = updatedList, selectedRequest = selected, isLoading = false)
                            }
                        }
                        addJob(job)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                addListener(listener)
            }
            .launchIn(viewModelScope)
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
                dataManager.deleteSupportRequest(event.requestId)
                commentController.stop()
                _uiState.update { it.copy(currentSubScreen = SupportScreenTypeEnum.HUB, selectedRequest = null) }
            }
            SupportEvent.ResetStatus -> _uiState.update { it.copy(isSuccess = false) }
            SupportEvent.Refresh -> performRefresh {
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
                    dataManager.getSupportColl().orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                } else {
                    dataManager.getSupportColl().whereEqualTo("userId", settings.getAppUserId()).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                }
                
                val snapshot = query.get(com.google.firebase.firestore.Source.SERVER).await()
                val list = snapshot.documents.mapNotNull { it.toObject(SupportRequest::class.java)?.copy(id = it.id) }
                
                val listWithCounts = list.map { req ->
                    viewModelScope.async {
                        try {
                            val countSnap = dataManager.getSupportColl().document(req.id).collection("comments").count()
                                .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                            req.apply { commentCount = countSnap.count.toInt() }
                        } catch (e: Exception) {
                            req
                        }
                    }
                }.awaitAll()
                
                _uiState.update { it.copy(requests = listWithCounts) }
            }
            is SupportEvent.Navigate -> {
                if (event.screen == SupportScreenTypeEnum.HUB) commentController.stop()
                _uiState.update { it.copy(currentSubScreen = event.screen) }
            }
        }
    }

    private fun openRequest(request: SupportRequest) {
        viewModelScope.launch {
            try {
                val countSnap = dataManager.getSupportColl().document(request.id).collection("comments").count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                request.commentCount = countSnap.count.toInt()
            } catch (e: Exception) { }
            _uiState.update { it.copy(selectedRequest = request, currentSubScreen = SupportScreenTypeEnum.DETAIL) }
            commentController.observe(dataManager.getSupportColl().document(request.id).collection("comments"))
        }
    }

    private fun updateStatus(requestId: String, status: SupportStatusEnum) {
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
                dataManager.updateSupportStatus(requestId, status)
            } catch (e: Exception) {
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

    private fun updateType(requestId: String, type: SupportTypeEnum) {
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
                dataManager.updateSupportType(requestId, type)
            } catch (e: Exception) {
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

    private fun submitRequest(type: SupportTypeEnum, context: android.content.Context) {
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
                    status = SupportStatusEnum.OPENED
                )
                
                val success = dataManager.submitSupportRequest(request, settings.getUserId())
                if (success) {
                    _uiState.update { it.copy(
                        isSending = false, 
                        isSuccess = true, 
                        requestText = "", 
                        currentSubScreen = SupportScreenTypeEnum.HUB,
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
        commentController.stop()
    }
}
