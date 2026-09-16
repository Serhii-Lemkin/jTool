package com.jtool.android.features.support.logic

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.sync.BaseSyncViewModel
import com.jtool.android.core.utils.DeviceInfoProvider
import com.jtool.android.core.sync.helpers.FirestoreCommentRepository
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

    private val commentController = CommentController(FirestoreCommentRepository(), viewModelScope) { settings.getAppUserId() }
    val comments = commentController.comments

    init {
        loadConfig()
        loadFilters()
        observeAdminStatus()
        observeRequests()
        
        isLoading.onEach { loading -> _uiState.update { it.copy(isLoading = loading) } }.launchIn(viewModelScope)
    }

    private fun loadFilters() {
        val types = settings.getSupportTypeFilters().mapNotNull { 
            try { SupportTypeEnum.valueOf(it) } catch(e:Exception) { null }
        }.toSet()
        val statuses = settings.getSupportStatusFilters().mapNotNull { 
            try { SupportStatusEnum.valueOf(it) } catch(e:Exception) { null }
        }.toSet()
        
        _uiState.update { it.copy(
            activeTypeFilters = types,
            activeStatusFilters = if (statuses.isEmpty()) setOf(SupportStatusEnum.OPENED, SupportStatusEnum.IN_PROGRESS) else statuses
        ) }
    }

    private fun saveFilters() {
        settings.saveSupportTypeFilters(_uiState.value.activeTypeFilters.map { it.name }.toSet())
        settings.saveSupportStatusFilters(_uiState.value.activeStatusFilters.map { it.name }.toSet())
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
                _isLoading.update { true }
                val userId = settings.getUserId()
                val coll = dataManager.getSupportColl()

                val query = if (isAdmin) {
                    coll.orderBy("timestamp", Query.Direction.DESCENDING)
                } else {
                    coll.whereEqualTo("userId", userId)
                }

                val listener = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("SupportVM", "Snapshot listener error: ${error.message}", error)
                        viewModelScope.launch { fetchSupportRequestsInternal(fromServer = false) }
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        cancelJobs()
                        val job = viewModelScope.launch {
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(SupportRequest::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    null
                                }
                            }.sortedByDescending { it.timestamp }

                            val listWithCounts = list.map { req ->
                                viewModelScope.async {
                                    try {
                                        val countSnap = dataManager.getSupportColl().document(req.id).collection("comments").count()
                                            .get(AggregateSource.SERVER).await()
                                        req.apply { commentCount = countSnap.count.toInt() }
                                    } catch (_: Exception) {
                                        req
                                    }
                                }
                            }.awaitAll()

                            _uiState.update { state ->
                                val selected = listWithCounts.find { it.id == state.selectedRequest?.id } ?: state.selectedRequest
                                state.copy(requests = listWithCounts, selectedRequest = selected, isLoading = false)
                            }
                            _isLoading.update { false }
                        }
                        addJob(job)
                    } else {
                        _isLoading.update { false }
                    }
                }
                addListener(listener)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun fetchSupportRequestsInternal(fromServer: Boolean) {
        val isAdmin = _uiState.value.isAdmin
        val userId = settings.getUserId()
        val coll = dataManager.getSupportColl()

        val documents = try {
            if (isAdmin) {
                val q = coll.orderBy("timestamp", Query.Direction.DESCENDING)
                if (fromServer) q.get(Source.SERVER).await().documents
                else q.get().await().documents
            } else {
                try {
                    val q = coll.whereEqualTo("userId", userId).orderBy("timestamp", Query.Direction.DESCENDING)
                    if (fromServer) q.get(Source.SERVER).await().documents
                    else q.get().await().documents
                } catch (e: Exception) {
                    val q = coll.whereEqualTo("userId", userId)
                    if (fromServer) q.get(Source.SERVER).await().documents
                    else q.get().await().documents
                }
            }
        } catch (e: Exception) {
            Log.e("SupportVM", "Error fetching requests: ${e.message}", e)
            emptyList()
        }

        val list = documents.mapNotNull { doc ->
            try {
                doc.toObject(SupportRequest::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }

        val listWithCounts = list.map { req ->
            viewModelScope.async {
                try {
                    val countSnap = dataManager.getSupportColl().document(req.id).collection("comments").count()
                        .get(AggregateSource.SERVER).await()
                    req.apply { commentCount = countSnap.count.toInt() }
                } catch (_: Exception) {
                    req
                }
            }
        }.awaitAll()

        _uiState.update { state ->
            val selected = listWithCounts.find { it.id == state.selectedRequest?.id } ?: state.selectedRequest
            state.copy(requests = listWithCounts, selectedRequest = selected, isLoading = false)
        }
    }

    fun onEvent(event: SupportEvent) {
        when (event) {
            is SupportEvent.ToggleTypeFilter -> {
                val current = _uiState.value.activeTypeFilters
                val next = if (current.contains(event.type)) current - event.type else current + event.type
                _uiState.update { it.copy(activeTypeFilters = next, pagingLimit = 15) }
                saveFilters()
            }
            is SupportEvent.ToggleStatusFilter -> {
                val current = _uiState.value.activeStatusFilters
                val next = if (current.contains(event.status)) current - event.status else current + event.status
                _uiState.update { it.copy(activeStatusFilters = next, pagingLimit = 15) }
                saveFilters()
            }
            SupportEvent.ClearFilters -> {
                _uiState.update { it.copy(
                    activeTypeFilters = emptySet(), 
                    activeStatusFilters = setOf(SupportStatusEnum.OPENED, SupportStatusEnum.IN_PROGRESS),
                    pagingLimit = 15 
                ) }
                saveFilters()
            }
            is SupportEvent.SetFilterType -> { }
            is SupportEvent.SetNewRequestType -> {
                _uiState.update { it.copy(newRequestType = event.type) }
            }
            is SupportEvent.ToggleArchived -> { }
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
                        searchQuery = "",
                        isSearchVisible = false,
                        pagingLimit = 15
                    )
                }
                loadFilters()
                fetchSupportRequestsInternal(fromServer = true)
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
                    userId = settings.getUserId(),
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
                        searchQuery = "",
                        isSearchVisible = false,
                        pagingLimit = 15
                    ) }
                } else {
                    _uiState.update { it.copy(isSending = false, error = "Submission failed") }
                }
            } catch (e: Exception) {
                val errorMessage = if (e.message == "LIMIT_REACHED") {
                    Loc.Support.daily_limit.format(uiState.value.maxRequests, uiState.value.maxRequests)
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
