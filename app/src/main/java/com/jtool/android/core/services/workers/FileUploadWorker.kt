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
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.core.services.FileUploadQueueManager
import com.jtool.android.core.services.UploadFileStatus
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.vaults.common.GoogleDriveVaultHelper
import com.jtool.android.features.vaults.common.VaultDocUtils
import java.io.File

class FileUploadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val dataManager = DataManager(appContext)
    private val settingsManager = SettingsManager(appContext)

    override suspend fun doWork(): Result {
        createNotificationChannel()
        processUploadQueue()
        return Result.success()
    }

    private suspend fun processUploadQueue() {
        while (true) {
            if (!VaultDocUtils.isNetworkAvailable(appContext)) {
                Log.w("FileUploadWorker", "No internet connection. Waiting for network reconnection...")
                updateNotification("Offline. Upload will resume when connected.", 0)
                break
            }

            val state = FileUploadQueueManager.uploadState.value
            val pendingTask = state.tasks.firstOrNull { !it.isFailed && it.files.any { f -> f.status == UploadFileStatus.PENDING || f.status == UploadFileStatus.UPLOADING } }

            if (pendingTask == null) {
                FileUploadQueueManager.markQueueFinished()
                cancelNotification()
                break
            }

            try {
                val gid = settingsManager.getCurrentGroupId() ?: ""
                val memberEmails = dataManager.getGroupMemberEmails(gid)

                val completedFiles = pendingTask.files.filter { it.status == UploadFileStatus.COMPLETED }
                val uploadedFileIds = completedFiles.mapNotNull { it.driveFileId }.toMutableList()
                val uploadedDriveUrls = completedFiles.mapNotNull { it.driveUrl }.toMutableList()
                val fileNames = completedFiles.map { "${it.fileName}${it.extension}" }.toMutableList()
                val fileTypes = completedFiles.map { it.extension.removePrefix(".") }.toMutableList()

                var docFolderId = pendingTask.createdFolderId ?: ""
                var docFolderUrl = pendingTask.createdFolderUrl ?: ""

                val targetFolderId = if (pendingTask.files.size > 1) {
                    if (docFolderId.isNotEmpty()) {
                        docFolderId
                    } else {
                        val (fId, fUrl) = GoogleDriveVaultHelper.createSubFolder(
                            appContext,
                            pendingTask.vault.driveFolderId,
                            pendingTask.docName,
                            memberEmails
                        )
                        docFolderId = fId ?: ""
                        docFolderUrl = fUrl ?: ""
                        if (docFolderId.isNotEmpty()) {
                            FileUploadQueueManager.setTaskFolder(pendingTask.id, docFolderId, docFolderUrl)
                        }
                        fId ?: pendingTask.vault.driveFolderId
                    }
                } else {
                    pendingTask.vault.driveFolderId
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
                        context = appContext,
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
                        FileUploadQueueManager.updateFileProgress(
                            pendingTask.id,
                            f.id,
                            f.fileSize,
                            100,
                            UploadFileStatus.COMPLETED,
                            driveFileId = uploadResult.first,
                            driveUrl = uploadResult.second
                        )
                        uploadedFileIds.add(uploadResult.first)
                        uploadedDriveUrls.add(uploadResult.second)
                        fileNames.add(fullFileName)
                        fileTypes.add(f.extension.removePrefix("."))
                    }
                }

                if (taskFailed) {
                    // Cleanup remote Drive garbage only if no files succeeded
                    if (uploadedFileIds.isEmpty() && docFolderId.isNotEmpty()) {
                        GoogleDriveVaultHelper.deleteRemoteDriveItem(appContext, docFolderId)
                        FileUploadQueueManager.setTaskFolder(pendingTask.id, "", "")
                    } else if (docFolderId.isEmpty()) {
                        uploadedFileIds.forEach { id ->
                            GoogleDriveVaultHelper.deleteRemoteDriveItem(appContext, id)
                        }
                    }
                    fileNames.forEach { fName ->
                        val srcFile = File(appContext.filesDir, "vaults/${pendingTask.vault.id}/$fName")
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
                            val docDir = File(appContext.filesDir, "vaults/${pendingTask.vault.id}/$docId")
                            if (!docDir.exists()) docDir.mkdirs()
                            fileNames.forEach { fileName ->
                                val srcFile = File(appContext.filesDir, "vaults/${pendingTask.vault.id}/$fileName")
                                if (srcFile.exists()) {
                                    srcFile.copyTo(File(docDir, fileName), overwrite = true)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FileUploadWorker", "Failed local offline caching", e)
                        }
                    }

                    fileNames.forEach { fileName ->
                        val srcFile = File(appContext.filesDir, "vaults/${pendingTask.vault.id}/$fileName")
                        if (srcFile.exists()) srcFile.delete()
                    }

                    FileUploadQueueManager.markTaskCompleted(pendingTask.id)
                    showSuccessNotification(pendingTask.docName)
                }
            } catch (e: Exception) {
                Log.e("FileUploadWorker", "Task processing error", e)
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
            val manager = appContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String, progressPercent: Int) =
        NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("jTools File Upload")
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

    private fun showSuccessNotification(docName: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Upload Complete")
            .setContentText("'$docName' uploaded successfully")
            .setSmallIcon(R.drawable.ic_jtool_launcher)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()
        manager.notify(docName.hashCode(), notif)
    }

    private fun showFailureNotification(docName: String, errorMsg: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Upload Failed")
            .setContentText("'$docName': $errorMsg. Tap to view and retry.")
            .setSmallIcon(R.drawable.ic_jtool_launcher)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()
        manager.notify(docName.hashCode(), notif)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_UPLOAD_PROGRESS", true)
        }
        return PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "file_uploads_channel"
        private const val NOTIFICATION_ID = 8801
    }
}
