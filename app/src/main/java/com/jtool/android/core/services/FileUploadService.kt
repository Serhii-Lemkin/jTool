package com.jtool.android.core.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jtool.android.MainActivity
import com.jtool.android.R
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import kotlinx.coroutines.*
import java.io.File

class FileUploadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var dataManager: DataManager
    private lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        dataManager = DataManager(applicationContext)
        settingsManager = SettingsManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Uploading files...", 0)
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            processUploadQueue()
        }

        return START_NOT_STICKY
    }

    private suspend fun processUploadQueue() {
        while (true) {
            if (!VaultDocUtils.isNetworkAvailable(applicationContext)) {
                Log.w("FileUploadService", "No internet connection. Waiting for network reconnection...")
                updateNotification("Offline. Upload will resume when connected.", 0)
                delay(5000)
                continue
            }

            val state = FileUploadQueueManager.uploadState.value
            val pendingTask = state.tasks.firstOrNull { !it.isFailed && it.files.any { f -> f.status == UploadFileStatus.PENDING || f.status == UploadFileStatus.UPLOADING } }

            if (pendingTask == null) {
                FileUploadQueueManager.markQueueFinished()
                stopForeground(true)
                stopSelf()
                break
            }

            try {
                val gid = settingsManager.getCurrentGroupId() ?: ""
                val memberEmails = dataManager.getGroupMemberEmails(gid)

                val uploadedFileIds = mutableListOf<String>()
                val uploadedDriveUrls = mutableListOf<String>()
                val fileNames = mutableListOf<String>()
                val fileTypes = mutableListOf<String>()

                var docFolderId = ""
                var docFolderUrl = ""
                val targetFolderId = run {
                    val (fId, fUrl) = GoogleDriveVaultHelper.createSubFolder(
                        applicationContext,
                        pendingTask.vault.driveFolderId,
                        pendingTask.docName,
                        memberEmails
                    )
                    docFolderId = fId ?: ""
                    docFolderUrl = fUrl ?: ""
                    fId ?: pendingTask.vault.driveFolderId
                }

                var taskFailed = false
                var errorMsg = ""

                var lastNotifTime = 0L
                for (f in pendingTask.files) {
                    if (f.status == UploadFileStatus.COMPLETED) {
                        continue
                    }

                    FileUploadQueueManager.updateFileProgress(pendingTask.id, f.id, 0L, 10, UploadFileStatus.UPLOADING)
                    val fullFileName = "${f.fileName}${f.extension}"

                    val notifText = if (pendingTask.files.size > 1) {
                        "'${pendingTask.docName}' (${pendingTask.files.size} files): $fullFileName"
                    } else {
                        "'${pendingTask.docName}': $fullFileName"
                    }
                    updateNotification(notifText, FileUploadQueueManager.uploadState.value.overallProgress)

                    val (uploadResult, err) = GoogleDriveVaultHelper.uploadDocumentToDrive(
                        context = applicationContext,
                        parentFolderId = targetFolderId,
                        sourceUri = f.uri,
                        fileName = fullFileName,
                        fileType = "document",
                        groupMemberEmails = memberEmails,
                        vaultId = pendingTask.vault.id,
                        onProgress = { bytesCopied ->
                            val p = if (f.fileSize > 0) ((bytesCopied.toDouble() / f.fileSize) * 100).toInt().coerceIn(0, 99) else 0
                            FileUploadQueueManager.updateFileProgress(pendingTask.id, f.id, bytesCopied, p, UploadFileStatus.UPLOADING)
                            
                            val now = System.currentTimeMillis()
                            if (now - lastNotifTime > 1000) {
                                lastNotifTime = now
                                updateNotification(notifText, FileUploadQueueManager.uploadState.value.overallProgress)
                            }
                        }
                    )

                    if (uploadResult == null) {
                        taskFailed = true
                        errorMsg = err ?: "Failed to upload $fullFileName"
                        FileUploadQueueManager.updateFileProgress(pendingTask.id, f.id, 0L, 0, UploadFileStatus.FAILED)
                        break
                    } else {
                        FileUploadQueueManager.updateFileProgress(pendingTask.id, f.id, f.fileSize, 100, UploadFileStatus.COMPLETED)
                        uploadedFileIds.add(uploadResult.first)
                        uploadedDriveUrls.add(uploadResult.second)
                        fileNames.add(fullFileName)
                        fileTypes.add(f.extension.removePrefix("."))
                    }
                }

                if (taskFailed) {
                    // Cleanup remote Drive garbage
                    if (docFolderId.isNotEmpty()) {
                        GoogleDriveVaultHelper.deleteRemoteDriveItem(applicationContext, docFolderId)
                    } else {
                        uploadedFileIds.forEach { id ->
                            GoogleDriveVaultHelper.deleteRemoteDriveItem(applicationContext, id)
                        }
                    }
                    fileNames.forEach { fName ->
                        val srcFile = File(applicationContext.filesDir, "vaults/${pendingTask.vault.id}/$fName")
                        if (srcFile.exists()) srcFile.delete()
                    }

                    FileUploadQueueManager.markTaskFailed(pendingTask.id, errorMsg)
                    showFailureNotification(pendingTask.docName, errorMsg)
                } else {
                    val newDoc = VaultDocument(
                        vaultId = pendingTask.vault.id,
                        name = pendingTask.docName,
                        tag = pendingTask.tag,
                        uploadTimestamp = System.currentTimeMillis(),
                        documentTimestamp = pendingTask.docDate,
                        description = pendingTask.description,
                        driveFileId = uploadedFileIds.firstOrNull() ?: "",
                        driveUrl = uploadedDriveUrls.firstOrNull() ?: "",
                        fileType = fileTypes.firstOrNull() ?: "",
                        driveFileIds = uploadedFileIds,
                        driveUrls = uploadedDriveUrls,
                        fileNames = fileNames,
                        fileTypes = fileTypes,
                        folderId = docFolderId,
                        folderUrl = docFolderUrl,
                        lastUpdatedBy = settingsManager.getAppUserId()
                    )

                    val docId = dataManager.addVaultDoc(gid, pendingTask.vault.id, newDoc)

                    if (pendingTask.saveOffline) {
                        try {
                            val docDir = File(applicationContext.filesDir, "vaults/${pendingTask.vault.id}/$docId")
                            if (!docDir.exists()) docDir.mkdirs()
                            fileNames.forEach { fileName ->
                                val srcFile = File(applicationContext.filesDir, "vaults/${pendingTask.vault.id}/$fileName")
                                if (srcFile.exists()) {
                                    srcFile.copyTo(File(docDir, fileName), overwrite = true)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FileUploadService", "Failed local offline caching", e)
                        }
                    }

                    fileNames.forEach { fileName ->
                        val srcFile = File(applicationContext.filesDir, "vaults/${pendingTask.vault.id}/$fileName")
                        if (srcFile.exists()) srcFile.delete()
                    }

                    FileUploadQueueManager.markTaskCompleted(pendingTask.id)
                    showSuccessNotification(pendingTask.docName)
                }
            } catch (e: Exception) {
                Log.e("FileUploadService", "Task processing error", e)
                FileUploadQueueManager.markTaskFailed(pendingTask.id, e.message ?: "Upload error")
                showFailureNotification(pendingTask.docName, e.message ?: "Upload error")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background file uploads to Google Drive"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String, progressPercent: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("jTools File Upload")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_jtool_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(100, progressPercent, progressPercent == 0)
            .setContentIntent(createPendingIntent())
            .build()

    private fun updateNotification(contentText: String, progressPercent: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(contentText, progressPercent))
    }

    private fun showSuccessNotification(docName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Upload Complete")
            .setContentText("'$docName' uploaded successfully")
            .setSmallIcon(R.drawable.ic_jtool_launcher)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()
        manager.notify(docName.hashCode(), notif)
    }

    private fun showFailureNotification(docName: String, errorMsg: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Upload Failed")
            .setContentText("'$docName': $errorMsg. Tap to view and retry.")
            .setSmallIcon(R.drawable.ic_jtool_launcher)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()
        manager.notify(docName.hashCode(), notif)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_UPLOAD_PROGRESS", true)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "file_uploads_channel"
        private const val NOTIFICATION_ID = 8801
    }
}
