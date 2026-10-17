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

enum class ExportFileStatus { PENDING, EXPORTING, COMPLETED, FAILED }

data class ExportFileProgress(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val fileIndex: Int,
    val progressPercent: Int = 0,
    val status: ExportFileStatus = ExportFileStatus.PENDING,
    val errorMessage: String? = null
)

data class ExportTask(
    val id: String = UUID.randomUUID().toString(),
    val doc: VaultDocument,
    val files: List<ExportFileProgress>,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

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
    private val _exportState = MutableStateFlow(ExportState())
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun enqueue(context: Context, doc: VaultDocument, fileIndex: Int? = null) {
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

        val intent = Intent(context, FileExportService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
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
    }

    fun markTaskFailed(taskId: String, errorMsg: String) {
        _exportState.update { state ->
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) task.copy(isFailed = true, errorMessage = errorMsg) else task
            }
            state.copy(tasks = updatedTasks)
        }
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
    }

    fun clearCompletedTasks() {
        _exportState.update { state ->
            val remainingTasks = state.tasks.filter { task ->
                task.isFailed || task.files.any { f -> f.status != ExportFileStatus.COMPLETED }
            }
            state.copy(
                tasks = remainingTasks,
                isExporting = remainingTasks.any { !it.isFailed && it.files.any { f -> f.status != ExportFileStatus.COMPLETED } }
            )
        }
    }

    fun retryTask(context: Context, taskId: String) {
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

        val intent = Intent(context, FileExportService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun markQueueFinished() {
        _exportState.update { it.copy(isExporting = false) }
    }
}
