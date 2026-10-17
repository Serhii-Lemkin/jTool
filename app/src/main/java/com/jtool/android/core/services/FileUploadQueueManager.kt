package com.jtool.android.core.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.features.vaults.AttachedFile
import com.jtool.android.features.vaults.common.VaultDocUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class UploadFileStatus { PENDING, UPLOADING, COMPLETED, FAILED }

data class UploadFileProgress(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val fileName: String,
    val extension: String,
    val fileSize: Long,
    val bytesUploaded: Long = 0L,
    val progressPercent: Int = 0,
    val status: UploadFileStatus = UploadFileStatus.PENDING,
    val errorMessage: String? = null
)

data class UploadTask(
    val id: String = UUID.randomUUID().toString(),
    val vault: Vault,
    val docName: String,
    val tag: String,
    val docDate: Long,
    val description: String,
    val saveOffline: Boolean,
    val files: List<UploadFileProgress>,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

data class UploadState(
    val tasks: List<UploadTask> = emptyList(),
    val isUploading: Boolean = false
) {
    val overallProgress: Int
        get() {
            val allFiles = tasks.flatMap { it.files }
            if (allFiles.isEmpty()) return 0
            val totalBytes = allFiles.sumOf { it.fileSize }
            if (totalBytes <= 0) return 0
            val uploadedBytes = allFiles.sumOf { it.bytesUploaded }
            return ((uploadedBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
        }
}

object FileUploadQueueManager {
    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun enqueue(
        context: Context,
        vault: Vault,
        files: List<AttachedFile>,
        docName: String,
        tag: String,
        docDate: Long,
        description: String,
        saveOffline: Boolean
    ) {
        val fileProgressList = files.map { file ->
            val size = VaultDocUtils.getFileSizeFromUri(context, file.uri)
            UploadFileProgress(
                uri = file.uri,
                fileName = file.fileName,
                extension = file.extension,
                fileSize = size
            )
        }

        val task = UploadTask(
            vault = vault,
            docName = docName,
            tag = tag,
            docDate = docDate,
            description = description,
            saveOffline = saveOffline,
            files = fileProgressList
        )

        _uploadState.update { current ->
            current.copy(
                tasks = current.tasks + task,
                isUploading = true
            )
        }

        val intent = Intent(context, FileUploadService::class.java)
        if (files.isNotEmpty()) {
            val clipData = android.content.ClipData.newUri(context.contentResolver, "Uploads", files.first().uri)
            for (i in 1 until files.size) {
                clipData.addItem(android.content.ClipData.Item(files[i].uri))
            }
            intent.clipData = clipData
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun updateFileProgress(taskId: String, fileId: String, bytesUploaded: Long, percent: Int, status: UploadFileStatus) {
        _uploadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val updatedFiles = task.files.map { f ->
                        if (f.id == fileId) {
                            f.copy(
                                bytesUploaded = bytesUploaded,
                                progressPercent = percent,
                                status = status
                            )
                        } else f
                    }
                    task.copy(files = updatedFiles)
                } else task
            }
            state.copy(tasks = updatedTasks)
        }
    }

    fun markTaskFailed(taskId: String, errorMsg: String) {
        _uploadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    task.copy(isFailed = true, errorMessage = errorMsg)
                } else task
            }
            state.copy(tasks = updatedTasks)
        }
    }

    fun markTaskCompleted(taskId: String) {
        _uploadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val completedFiles = task.files.map { f -> f.copy(status = UploadFileStatus.COMPLETED, progressPercent = 100) }
                    task.copy(files = completedFiles)
                } else task
            }
            state.copy(
                tasks = updatedTasks,
                isUploading = updatedTasks.any { !it.isFailed && it.files.any { f -> f.status != UploadFileStatus.COMPLETED } }
            )
        }
    }

    fun clearCompletedTasks() {
        _uploadState.update { state ->
            val remainingTasks = state.tasks.filter { task ->
                task.isFailed || task.files.any { f -> f.status != UploadFileStatus.COMPLETED }
            }
            state.copy(
                tasks = remainingTasks,
                isUploading = remainingTasks.any { !it.isFailed && it.files.any { f -> f.status != UploadFileStatus.COMPLETED } }
            )
        }
    }

    fun cancelFileInTask(taskId: String, fileId: String) {
        _uploadState.update { state ->
            val updatedTasks = state.tasks.mapNotNull { task ->
                if (task.id == taskId) {
                    val updatedFiles = task.files.filter { it.id != fileId }
                    if (updatedFiles.isEmpty()) null
                    else task.copy(files = updatedFiles)
                } else task
            }
            state.copy(
                tasks = updatedTasks,
                isUploading = updatedTasks.any { !it.isFailed && it.files.any { f -> f.status != UploadFileStatus.COMPLETED } }
            )
        }
    }

    fun cancelTask(taskId: String) {
        removeTask(taskId)
    }

    fun removeTask(taskId: String) {
        _uploadState.update { state ->
            val updatedTasks = state.tasks.filter { it.id != taskId }
            state.copy(
                tasks = updatedTasks,
                isUploading = updatedTasks.any { !it.isFailed && it.files.any { f -> f.status != UploadFileStatus.COMPLETED } }
            )
        }
    }

    fun markQueueFinished() {
        _uploadState.update { it.copy(isUploading = false) }
    }
}
