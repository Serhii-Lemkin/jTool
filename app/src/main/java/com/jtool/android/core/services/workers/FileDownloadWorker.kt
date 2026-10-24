package com.jtool.android.core.services.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jtool.android.MainActivity
import com.jtool.android.R
import com.jtool.android.core.services.DownloadFileStatus
import com.jtool.android.core.services.FileDownloadQueueManager
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper

class FileDownloadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        createNotificationChannel()
        processDownloadQueue()
        return Result.success()
    }

    private suspend fun processDownloadQueue() {
        while (true) {
            val state = FileDownloadQueueManager.downloadState.value
            val pendingTask = state.tasks.firstOrNull { !it.isFailed && it.files.any { f -> f.status == DownloadFileStatus.PENDING || f.status == DownloadFileStatus.DOWNLOADING } }

            if (pendingTask == null) {
                FileDownloadQueueManager.markQueueFinished()
                cancelNotification()
                break
            }

            try {
                var taskFailed = false
                var errorMsg = ""

                for (f in pendingTask.files) {
                    if (f.status == DownloadFileStatus.COMPLETED) continue

                    FileDownloadQueueManager.updateFileProgress(pendingTask.id, f.fileIndex, 0, DownloadFileStatus.DOWNLOADING)
                    updateNotification("Caching '${f.fileName}'...", FileDownloadQueueManager.downloadState.value.overallProgress)

                    val localPath = GoogleDriveVaultHelper.downloadDocumentToLocal(
                        context = appContext,
                        vaultId = pendingTask.doc.vaultId,
                        doc = pendingTask.doc,
                        index = f.fileIndex,
                        onProgress = { _, percent ->
                            FileDownloadQueueManager.updateFileProgress(pendingTask.id, f.fileIndex, percent, DownloadFileStatus.DOWNLOADING)
                            updateNotification("Caching '${f.fileName}' ($percent%)...", FileDownloadQueueManager.downloadState.value.overallProgress)
                        }
                    )

                    if (localPath.isEmpty()) {
                        taskFailed = true
                        errorMsg = "Failed to download ${f.fileName}"
                        FileDownloadQueueManager.updateFileProgress(pendingTask.id, f.fileIndex, 0, DownloadFileStatus.FAILED)
                        break
                    } else {
                        FileDownloadQueueManager.updateFileProgress(pendingTask.id, f.fileIndex, 100, DownloadFileStatus.COMPLETED)
                    }
                }

                if (taskFailed) {
                    FileDownloadQueueManager.markTaskFailed(pendingTask.id, errorMsg)
                    showFailureNotification(pendingTask.doc.name, errorMsg)
                } else {
                    FileDownloadQueueManager.markTaskCompleted(pendingTask.id)
                }
            } catch (e: Exception) {
                Log.e("FileDownloadWorker", "Download task processing error", e)
                FileDownloadQueueManager.markTaskFailed(pendingTask.id, e.message ?: "Download error")
                showFailureNotification(pendingTask.doc.name, e.message ?: "Download error")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Offline File Caching",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of downloading files for offline view"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = appContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String, progressPercent: Int) =
        NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("jTools Offline Cache")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_jtool_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(100, progressPercent, progressPercent == 0)
            .setContentIntent(createPendingIntent())
            .build()

    private fun updateNotification(contentText: String, progressPercent: Int) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(contentText, progressPercent))
    }

    private fun cancelNotification() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    private fun showFailureNotification(docName: String, errorMsg: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Caching Failed")
            .setContentText("'$docName': $errorMsg")
            .setSmallIcon(R.drawable.ic_jtool_launcher)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()
        manager.notify(docName.hashCode(), notif)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "file_downloads_channel"
        private const val NOTIFICATION_ID = 8802
    }
}
