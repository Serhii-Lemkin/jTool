package com.jtool.android.core.services

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.services.workers.FileUploadWorker
import com.jtool.android.features.vaults.AttachedFile
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.features.vaults.common.VaultDocUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class UploadFileStatus { PENDING, UPLOADING, COMPLETED, FAILED }

@Serializable
data class UploadFileProgress(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String = "",
    val fileName: String = "",
    val extension: String = "",
    val fileSize: Long = 0L,
    val bytesUploaded: Long = 0L,
    val progressPercent: Int = 0,
    val status: UploadFileStatus = UploadFileStatus.PENDING,
    val errorMessage: String? = null,
    val driveFileId: String? = null,
    val driveUrl: String? = null
) {
    val uri: Uri get() = Uri.parse(uriString)
}

@Serializable
data class UploadTask(
    val id: String = UUID.randomUUID().toString(),
    val vault: Vault,
    val docName: String,
    val tag: String,
    val docDate: Long,
    val description: String,
    val saveOffline: Boolean,
    val files: List<UploadFileProgress>,
    val createdFolderId: String? = null,
    val createdFolderUrl: String? = null,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
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
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        loadState(context.applicationContext)
    }

    private fun saveState() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("jtool_upload_queue_prefs", Context.MODE_PRIVATE)
            val jsonString = json.encodeToString(_uploadState.value)
            prefs.edit().putString("upload_state", jsonString).apply()
        } catch (e: Exception) {
            Log.e("FileUploadQueueManager", "Failed to save upload state", e)
        }
    }

    private fun loadState(context: Context) {
        try {
            val prefs = context.getSharedPreferences("jtool_upload_queue_prefs", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("upload_state", null) ?: return
            val restored = json.decodeFromString<UploadState>(jsonString)
            
            val hasUnfinished = restored.tasks.any { !it.isFailed && it.files.any { f -> f.status != UploadFileStatus.COMPLETED } }
            val updatedState = restored.copy(isUploading = hasUnfinished)
            _uploadState.value = updatedState

            if (hasUnfinished) {
                try {
                    val workRequest = OneTimeWorkRequestBuilder<FileUploadWorker>()
                        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build()
                    WorkManager.getInstance(context).enqueueUniqueWork("file_upload_work", ExistingWorkPolicy.KEEP, workRequest)
                } catch (e: Exception) {
                    Log.e("FileUploadQueueManager", "Failed to enqueue WorkManager work", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FileUploadQueueManager", "Failed to load upload state, clearing corrupted state", e)
            try {
                context.getSharedPreferences("jtool_upload_queue_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Exception) {}
        }
    }

    fun setTaskFolder(taskId: String, folderId: String, folderUrl: String) {
        _uploadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    task.copy(createdFolderId = folderId, createdFolderUrl = folderUrl)
                } else task
            }
            state.copy(tasks = updatedTasks)
        }
        saveState()
    }

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
        appContext = context.applicationContext
        val fileProgressList = files.map { file ->
            val size = VaultDocUtils.getFileSizeFromUri(context, file.uri)
            UploadFileProgress(
                uriString = file.uri.toString(),
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
        saveState()

        try {
            val workRequest = OneTimeWorkRequestBuilder<FileUploadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("file_upload_work", ExistingWorkPolicy.KEEP, workRequest)
        } catch (e: Exception) {
            Log.e("FileUploadQueueManager", "Failed to enqueue WorkManager work", e)
        }
    }

    fun updateFileProgress(
        taskId: String,
        fileId: String,
        bytesUploaded: Long,
        percent: Int,
        status: UploadFileStatus,
        driveFileId: String? = null,
        driveUrl: String? = null
    ) {
        _uploadState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val updatedFiles = task.files.map { f ->
                        if (f.id == fileId) {
                            f.copy(
                                bytesUploaded = bytesUploaded,
                                progressPercent = percent,
                                status = status,
                                driveFileId = driveFileId ?: f.driveFileId,
                                driveUrl = driveUrl ?: f.driveUrl
                            )
                        } else f
                    }
                    task.copy(files = updatedFiles)
                } else task
            }
            state.copy(tasks = updatedTasks)
        }
        saveState()
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
        saveState()
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
        saveState()
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
        saveState()
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
        saveState()
    }

    fun cancelTask(taskId: String) {
        removeTask(taskId)
    }

    fun removeTask(taskId: String) {
        val taskToRemove = _uploadState.value.tasks.find { it.id == taskId }
        if (taskToRemove != null) {
            val ctx = appContext
            if (ctx != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val folderId = taskToRemove.createdFolderId
                    if (!folderId.isNullOrEmpty()) {
                        GoogleDriveVaultHelper.deleteRemoteDriveItem(ctx, folderId)
                    } else {
                        taskToRemove.files.mapNotNull { it.driveFileId }.forEach { id ->
                            GoogleDriveVaultHelper.deleteRemoteDriveItem(ctx, id)
                        }
                    }
                }
            }
        }
        _uploadState.update { state ->
            val updatedTasks = state.tasks.filter { it.id != taskId }
            state.copy(
                tasks = updatedTasks,
                isUploading = updatedTasks.any { !it.isFailed && it.files.any { f -> f.status != UploadFileStatus.COMPLETED } }
            )
        }
        saveState()
    }

    fun markQueueFinished() {
        _uploadState.update { it.copy(isUploading = false) }
        saveState()
    }
}
