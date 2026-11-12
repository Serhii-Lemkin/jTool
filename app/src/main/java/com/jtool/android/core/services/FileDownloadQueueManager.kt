package com.jtool.android.core.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.services.workers.FileDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class DownloadFileStatus { PENDING, DOWNLOADING, COMPLETED, FAILED }

@Serializable
data class DownloadFileProgress(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String = "",
    val fileIndex: Int = 0,
    val progressPercent: Int = 0,
    val status: DownloadFileStatus = DownloadFileStatus.PENDING
)

@Serializable
data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val doc: VaultDocument,
    val files: List<DownloadFileProgress>,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class DownloadState(
    val tasks: List<DownloadTask> = emptyList(),
    val isDownloading: Boolean = false
) {
    val overallProgress: Int
        get() {
            val allFiles = tasks.flatMap { it.files }
            if (allFiles.isEmpty()) return 0
            val completedCount = allFiles.count { it.status == DownloadFileStatus.COMPLETED }
            return ((completedCount.toDouble() / allFiles.size) * 100).toInt().coerceIn(0, 100)
        }
}

object FileDownloadQueueManager {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        loadState(context.applicationContext)
    }

    private fun saveState() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("jtool_download_queue_prefs", Context.MODE_PRIVATE)
            val jsonString = json.encodeToString(_downloadState.value)
            prefs.edit().putString("download_state", jsonString).apply()
        } catch (e: Exception) {
            Log.e("FileDownloadQueueManager", "Failed to save download state", e)
        }
    }

    private fun loadState(context: Context) {
        try {
            val prefs = context.getSharedPreferences("jtool_download_queue_prefs", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("download_state", null) ?: return
            val restored = json.decodeFromString<DownloadState>(jsonString)
            
            val hasUnfinished = restored.tasks.any { !it.isFailed && it.files.any { f -> f.status != DownloadFileStatus.COMPLETED } }
            val updatedState = restored.copy(isDownloading = hasUnfinished)
            _downloadState.value = updatedState

            if (hasUnfinished) {
                try {
                    val workRequest = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                        .build()
                    WorkManager.getInstance(context).enqueueUniqueWork("file_download_work", ExistingWorkPolicy.KEEP, workRequest)
                } catch (e: Exception) {
                    Log.e("FileDownloadQueueManager", "Failed to enqueue WorkManager work", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FileDownloadQueueManager", "Failed to load download state, clearing corrupted state", e)
            try {
                context.getSharedPreferences("jtool_download_queue_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Exception) {}
        }
    }

    fun enqueue(context: Context, doc: VaultDocument) {
        appContext = context.applicationContext
        val totalFiles = if (doc.fileNames.isNotEmpty()) doc.fileNames.size else if (doc.driveFileIds.isNotEmpty()) doc.driveFileIds.size else 1
        val files = (0 until totalFiles).map { i ->
            val fileName = doc.fileNames.getOrNull(i) ?: doc.name
            DownloadFileProgress(fileName = fileName, fileIndex = i)
        }

        val task = DownloadTask(doc = doc, files = files)

        _downloadState.update { current ->
            current.copy(
                tasks = current.tasks + task,
                isDownloading = true
            )
        }
        saveState()

        try {
            val workRequest = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("file_download_work", ExistingWorkPolicy.KEEP, workRequest)
        } catch (e: Exception) {
            Log.e("FileDownloadQueueManager", "Failed to enqueue WorkManager work", e)
        }
    }

    fun updateFileProgress(taskId: String, fileIndex: Int, percent: Int, status: DownloadFileStatus) {
        _downloadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val updatedFiles = task.files.map { f ->
                        if (f.fileIndex == fileIndex) {
                            f.copy(progressPercent = percent, status = status)
                        } else f
                    }
                    task.copy(files = updatedFiles)
                } else task
            }
            state.copy(tasks = updatedTasks)
        }
        saveState()
    }

    fun retryTask(context: Context, taskId: String) {
        appContext = context.applicationContext
        _downloadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val resetFiles = task.files.map { f ->
                        if (f.status == DownloadFileStatus.FAILED) f.copy(status = DownloadFileStatus.PENDING, progressPercent = 0)
                        else f
                    }
                    task.copy(files = resetFiles, isFailed = false, errorMessage = null)
                } else task
            }
            state.copy(tasks = updatedTasks, isDownloading = true)
        }
        saveState()

        try {
            val workRequest = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("file_download_work", ExistingWorkPolicy.KEEP, workRequest)
        } catch (e: Exception) {
            Log.e("FileDownloadQueueManager", "Failed to enqueue WorkManager work", e)
        }
    }

    fun markTaskFailed(taskId: String, errorMsg: String) {
        _downloadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) task.copy(isFailed = true, errorMessage = errorMsg) else task
            }
            state.copy(tasks = updatedTasks)
        }
        saveState()
    }

    fun markTaskCompleted(taskId: String) {
        _downloadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val completedFiles = task.files.map { f -> f.copy(status = DownloadFileStatus.COMPLETED, progressPercent = 100) }
                    task.copy(files = completedFiles)
                } else task
            }
            state.copy(
                tasks = updatedTasks,
                isDownloading = updatedTasks.any { !it.isFailed && it.files.any { f -> f.status != DownloadFileStatus.COMPLETED } }
            )
        }
        saveState()
    }

    fun clearCompletedTasks() {
        _downloadState.update { state ->
            val remainingTasks = state.tasks.filter { task ->
                task.isFailed || task.files.any { f -> f.status != DownloadFileStatus.COMPLETED }
            }
            state.copy(
                tasks = remainingTasks,
                isDownloading = remainingTasks.any { !it.isFailed && it.files.any { f -> f.status != DownloadFileStatus.COMPLETED } }
            )
        }
        saveState()
    }

    fun removeTask(taskId: String) {
        _downloadState.update { state ->
            val updatedTasks = state.tasks.filter { it.id != taskId }
            state.copy(
                tasks = updatedTasks,
                isDownloading = updatedTasks.any { !it.isFailed && it.files.any { f -> f.status != DownloadFileStatus.COMPLETED } }
            )
        }
        saveState()
    }

    fun clearFinishedTasks() {
        _downloadState.update { state ->
            val activeTasks = state.tasks.filter { task ->
                !task.isFailed && task.files.any { f -> f.status == DownloadFileStatus.PENDING || f.status == DownloadFileStatus.DOWNLOADING }
            }
            state.copy(
                tasks = activeTasks,
                isDownloading = activeTasks.isNotEmpty()
            )
        }
        saveState()
    }

    fun markQueueFinished() {
        _downloadState.update { it.copy(isDownloading = false) }
        saveState()
    }
}
