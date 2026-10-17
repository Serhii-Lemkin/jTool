package com.jtool.android.core.services

import android.content.Context
import android.content.Intent
import android.os.Build
import com.jtool.android.core.models.vaults.VaultDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class DownloadFileStatus { PENDING, DOWNLOADING, COMPLETED, FAILED }

data class DownloadFileProgress(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val fileIndex: Int,
    val progressPercent: Int = 0,
    val status: DownloadFileStatus = DownloadFileStatus.PENDING
)

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val doc: VaultDocument,
    val files: List<DownloadFileProgress>,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

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
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun enqueue(context: Context, doc: VaultDocument) {
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

        val intent = Intent(context, FileDownloadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
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
    }

    fun retryTask(context: Context, taskId: String) {
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

        val intent = Intent(context, FileDownloadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun markTaskFailed(taskId: String, errorMsg: String) {
        _downloadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) task.copy(isFailed = true, errorMessage = errorMsg) else task
            }
            state.copy(tasks = updatedTasks)
        }
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
    }

    fun removeTask(taskId: String) {
        _downloadState.update { state ->
            val updatedTasks = state.tasks.filter { it.id != taskId }
            state.copy(
                tasks = updatedTasks,
                isDownloading = updatedTasks.any { !it.isFailed && it.files.any { f -> f.status != DownloadFileStatus.COMPLETED } }
            )
        }
    }

    fun markQueueFinished() {
        _downloadState.update { it.copy(isDownloading = false) }
    }
}
