package com.jtool.android.core.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.jtool.android.core.utils.SyncUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseSyncViewModel : ViewModel() {

    protected val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    protected val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()
    private val jobs = mutableListOf<Job>()

    protected fun addListener(listener: ListenerRegistration) {
        listeners.add(listener)
    }

    protected fun addJob(job: Job) {
        jobs.add(job)
    }

    protected fun clearListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    protected fun cancelJobs() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    protected fun performRefresh(
        onRefreshStart: () -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            SyncUtils.performRefresh(
                onLoading = { loading -> 
                    _isRefreshing.update { loading }
                    _isLoading.update { loading }
                }
            ) {
                onRefreshStart()
                block()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearListeners()
        cancelJobs()
    }
}
