package com.jtool.android.core.services.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jtool.android.core.services.ExportFileStatus
import com.jtool.android.core.services.FileExportQueueManager
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.features.vaults.common.VaultDocUtils

class FileExportWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val NOTIFICATION_ID = 2003
        private const val CHANNEL_ID = "jtools_file_export"
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()
        processExportQueue()
        return Result.success()
    }

    private suspend fun processExportQueue() {
        while (true) {
            val state = FileExportQueueManager.exportState.value
            val pendingTask = state.tasks.firstOrNull { !it.isFailed && it.files.any { f -> f.status == ExportFileStatus.PENDING || f.status == ExportFileStatus.EXPORTING } }

            if (pendingTask == null) {
                FileExportQueueManager.markQueueFinished()
                cancelNotification()
                break
            }

            try {
                var taskFailed = false
                var errorMsg = ""

                for (f in pendingTask.files) {
                    if (f.status == ExportFileStatus.COMPLETED) continue

                    FileExportQueueManager.updateFileProgress(pendingTask.id, f.fileIndex, 0, ExportFileStatus.EXPORTING)
                    updateNotification("Exporting '${f.fileName}'...", FileExportQueueManager.exportState.value.overallProgress)

                    val localFile = VaultDocUtils.getOfflineLocalFile(appContext, pendingTask.doc, f.fileIndex)
                    val rawName = pendingTask.doc.fileNames.getOrNull(f.fileIndex) ?: pendingTask.doc.name
                    val type = pendingTask.doc.fileTypes.getOrNull(f.fileIndex) ?: pendingTask.doc.fileType

                    val success = if (localFile.exists() && localFile.length() > 0) {
                        GoogleDriveVaultHelper.saveToPublicDownloads(appContext, localFile.absolutePath, rawName, type)
                    } else {
                        GoogleDriveVaultHelper.saveRemoteDriveFileToPublicDownloads(appContext, pendingTask.doc, f.fileIndex)
                    }

                    if (!success) {
                        taskFailed = true
                        errorMsg = "Failed to save ${f.fileName} to Downloads"
                        FileExportQueueManager.updateFileProgress(pendingTask.id, f.fileIndex, 0, ExportFileStatus.FAILED)
                        break
                    } else {
                        FileExportQueueManager.updateFileProgress(pendingTask.id, f.fileIndex, 100, ExportFileStatus.COMPLETED)
                    }
                }

                if (taskFailed) {
                    FileExportQueueManager.markTaskFailed(pendingTask.id, errorMsg)
                } else {
                    FileExportQueueManager.markTaskCompleted(pendingTask.id)
                }
            } catch (e: Exception) {
                Log.e("FileExportWorker", "Export task processing error", e)
                FileExportQueueManager.markTaskFailed(pendingTask.id, e.message ?: "Export error")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of saving files to Downloads"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = appContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, progress: Int): android.app.Notification {
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .build()
    }

    private fun updateNotification(title: String, progress: Int) {
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(title, progress))
    }

    private fun cancelNotification() {
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
    }
}
