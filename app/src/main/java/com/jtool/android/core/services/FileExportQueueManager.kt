package com.jtool.android.core.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.services.workers.FileExportWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class ExportFileStatus { PENDING, EXPORTING, COMPLETED, FAILED }

@Serializable
data class ExportFileProgress(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String = "",
    val fileIndex: Int = 0,
    val progressPercent: Int = 0,
    val status: ExportFileStatus = ExportFileStatus.PENDING,
    val errorMessage: String? = null
)

@Serializable
data class ExportTask(
    val id: String = UUID.randomUUID().toString(),
    val doc: VaultDocument,
    val files: List<ExportFileProgress>,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class ExportState(
    val tasks: List<ExportTask> = emptyList(),
    val isExporting: Boolean = false
) {
    val overallProgress: Int
        get() {
            val allFiles = tasks.flatMap { it.files }
            if (allFiles.isEmpty()) return 0
            val completedCount = allFiles.count { it.status == ExportFileStatus.COMPLETED }
            return ((completedCount.toDouble() / allFiles.size) * 100).toInt().coerceIn(0, 100)
        }
}

object FileExportQueueManager {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    private val _exportState = MutableStateFlow(ExportState())
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        loadState(context.applicationContext)
    }

    private fun saveState() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("jtool_export_queue_prefs", Context.MODE_PRIVATE)
            val jsonString = json.encodeToString(_exportState.value)
            prefs.edit().putString("export_state", jsonString).apply()
        } catch (e: Exception) {
            Log.e("FileExportQueueManager", "Failed to save export state", e)
        }
    }

    private fun loadState(context: Context) {
        try {
            val prefs = context.getSharedPreferences("jtool_export_queue_prefs", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("export_state", null) ?: return
            val restored = json.decodeFromString<ExportState>(jsonString)
            
            val hasUnfinished = restored.tasks.any { !it.isFailed && it.files.any { f -> f.status != ExportFileStatus.COMPLETED } }
            val updatedState = restored.copy(isExporting = hasUnfinished)
            _exportState.value = updatedState

            if (hasUnfinished) {
                try {
                    val workRequest = OneTimeWorkRequestBuilder<FileExportWorker>()
                        .build()
                    WorkManager.getInstance(context).enqueueUniqueWork("file_export_work", ExistingWorkPolicy.KEEP, workRequest)
                } catch (e: Exception) {
                    Log.e("FileExportQueueManager", "Failed to enqueue WorkManager work", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FileExportQueueManager", "Failed to load export state, clearing corrupted state", e)
            try {
                context.getSharedPreferences("jtool_export_queue_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Exception) {}
        }
    }

    fun enqueue(context: Context, doc: VaultDocument, fileIndex: Int? = null) {
        appContext = context.applicationContext
        val indices = if (fileIndex != null) {
            listOf(fileIndex)
        } else {
            val count = if (doc.fileNames.isNotEmpty()) doc.fileNames.size else if (doc.driveFileIds.isNotEmpty()) doc.driveFileIds.size else 1
            (0 until count).toList()
        }

        val files = indices.map { i ->
            val fileName = doc.fileNames.getOrNull(i) ?: doc.name
            ExportFileProgress(fileName = fileName, fileIndex = i)
        }

        val task = ExportTask(doc = doc, files = files)

        _exportState.update { current ->
            current.copy(
                tasks = current.tasks + task,
                isExporting = true
            )
        }
        saveState()

        try {
            val workRequest = OneTimeWorkRequestBuilder<FileExportWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("file_export_work", ExistingWorkPolicy.KEEP, workRequest)
        } catch (e: Exception) {
            Log.e("FileExportQueueManager", "Failed to enqueue WorkManager work", e)
        }
    }

    fun updateFileProgress(taskId: String, fileIndex: Int, percent: Int, status: ExportFileStatus) {
        _exportState.update { state ->
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

    fun markTaskFailed(taskId: String, errorMsg: String) {
        _exportState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) task.copy(isFailed = true, errorMessage = errorMsg) else task
            }
            state.copy(tasks = updatedTasks)
        }
        saveState()
    }

    fun markTaskCompleted(taskId: String) {
        _exportState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val completedFiles = task.files.map { f -> f.copy(status = ExportFileStatus.COMPLETED, progressPercent = 100) }
                    task.copy(files = completedFiles)
                } else task
            }
            state.copy(
                tasks = updatedTasks,
                isExporting = updatedTasks.any { !it.isFailed && it.files.any { f -> f.status != ExportFileStatus.COMPLETED } }
            )
        }
        saveState()
    }

    fun clearCompletedTasks() {
        _exportState.update { state ->
            val remainingTasks = state.tasks.filter { task ->
                !task.isFailed && task.files.any { f -> f.status == ExportFileStatus.PENDING || f.status == ExportFileStatus.EXPORTING }
            }
            state.copy(
                tasks = remainingTasks,
                isExporting = remainingTasks.isNotEmpty()
            )
        }
        saveState()
    }

    fun retryTask(context: Context, taskId: String) {
        appContext = context.applicationContext
        _exportState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    val resetFiles = task.files.map { f ->
                        if (f.status == ExportFileStatus.FAILED) f.copy(status = ExportFileStatus.PENDING, progressPercent = 0)
                        else f
                    }
                    task.copy(files = resetFiles, isFailed = false, errorMessage = null)
                } else task
            }
            state.copy(tasks = updatedTasks, isExporting = true)
        }
        saveState()

        try {
            val workRequest = OneTimeWorkRequestBuilder<FileExportWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("file_export_work", ExistingWorkPolicy.KEEP, workRequest)
        } catch (e: Exception) {
            Log.e("FileExportQueueManager", "Failed to enqueue WorkManager work", e)
        }
    }

    fun markQueueFinished() {
        _exportState.update { it.copy(isExporting = false) }
        saveState()
    }
}
