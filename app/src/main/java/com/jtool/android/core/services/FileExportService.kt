package com.jtool.android.core.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.features.vaults.common.VaultDocUtils
import kotlinx.coroutines.*

class FileExportService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val NOTIFICATION_ID = 2003
        private const val CHANNEL_ID = "jtools_file_export"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Saving files to Downloads...", 0)
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            processExportQueue()
        }

        return START_NOT_STICKY
    }

    private suspend fun processExportQueue() {
        while (true) {
            val state = FileExportQueueManager.exportState.value
            val pendingTask = state.tasks.firstOrNull { !it.isFailed && it.files.any { f -> f.status == ExportFileStatus.PENDING || f.status == ExportFileStatus.EXPORTING } }

            if (pendingTask == null) {
                FileExportQueueManager.markQueueFinished()
                stopForeground(true)
                stopSelf()
                break
            }

            try {
                var taskFailed = false
                var errorMsg = ""

                for (f in pendingTask.files) {
                    if (f.status == ExportFileStatus.COMPLETED) continue

                    FileExportQueueManager.updateFileProgress(pendingTask.id, f.fileIndex, 0, ExportFileStatus.EXPORTING)
                    updateNotification("Exporting '${f.fileName}'...", FileExportQueueManager.exportState.value.overallProgress)

                    val localFile = VaultDocUtils.getOfflineLocalFile(applicationContext, pendingTask.doc, f.fileIndex)
                    val rawName = pendingTask.doc.fileNames.getOrNull(f.fileIndex) ?: pendingTask.doc.name
                    val type = pendingTask.doc.fileTypes.getOrNull(f.fileIndex) ?: pendingTask.doc.fileType

                    val success = if (localFile.exists() && localFile.length() > 0) {
                        GoogleDriveVaultHelper.saveToPublicDownloads(applicationContext, localFile.absolutePath, rawName, type)
                    } else {
                        GoogleDriveVaultHelper.saveRemoteDriveFileToPublicDownloads(applicationContext, pendingTask.doc, f.fileIndex)
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
                Log.e("FileExportService", "Export task processing error", e)
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
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, progress: Int): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .build()
    }

    private fun updateNotification(title: String, progress: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(title, progress))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
