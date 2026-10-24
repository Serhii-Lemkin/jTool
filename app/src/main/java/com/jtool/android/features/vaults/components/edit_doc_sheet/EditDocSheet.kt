package com.jtool.android.features.vaults.components.edit_doc_sheet

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.utils.rememberCustomDatePickerState
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.features.vaults.AttachedFile
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.ui.components.common.GenericSheet
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ExistingFileItem(
    val driveFileId: String,
    val driveUrl: String,
    var fileName: String,
    val extension: String,
    val fileType: String
)

fun getExistingFileItems(doc: VaultDocument): List<ExistingFileItem> {
    if (doc.driveFileIds.isNotEmpty()) {
        return doc.driveFileIds.indices.map { i ->
            val fullName = doc.fileNames.getOrElse(i) { doc.name }
            val ext = if (fullName.contains('.')) ".${fullName.substringAfterLast('.')}" else ""
            val nameWithoutExt = if (fullName.contains('.')) fullName.substringBeforeLast('.') else fullName
            ExistingFileItem(
                driveFileId = doc.driveFileIds[i],
                driveUrl = doc.driveUrls.getOrElse(i) { "" },
                fileName = nameWithoutExt,
                extension = ext,
                fileType = doc.fileTypes.getOrElse(i) { doc.fileType }
            )
        }.filter { it.driveFileId.isNotEmpty() }
    } else if (doc.driveFileId.isNotEmpty()) {
        val ext = if (doc.name.contains('.')) ".${doc.name.substringAfterLast('.')}" else ""
        val nameWithoutExt = if (doc.name.contains('.')) doc.name.substringBeforeLast('.') else doc.name
        return listOf(
            ExistingFileItem(
                driveFileId = doc.driveFileId,
                driveUrl = doc.driveUrl,
                fileName = nameWithoutExt,
                extension = ext,
                fileType = doc.fileType
            )
        )
    }
    return emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDocSheet(
    vault: Vault,
    doc: VaultDocument,
    isUploading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (VaultDocument, List<AttachedFile>, List<String>) -> Unit
) {
    val context = LocalContext.current
    var docName by remember { mutableStateOf(doc.name) }
    var tag by remember { mutableStateOf(VaultDocUtils.getLocalizedTag(doc.tag)) }
    var tagInput by remember { mutableStateOf(VaultDocUtils.getLocalizedTag(doc.tag)) }
    var docDateMs by remember { mutableStateOf(doc.documentTimestamp) }
    var description by remember { mutableStateOf(doc.description) }
    var showDatePicker by remember { mutableStateOf(false) }

    var existingFiles by remember { mutableStateOf(getExistingFileItems(doc)) }
    var removedDriveIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var newFiles by remember { mutableStateOf<List<AttachedFile>>(emptyList()) }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[/\\\\:*?\"<>|]"), "")
    }

    val tagsMap = remember {
        mapOf(
            "#WARRANTY#" to VaultsLoc.tagWarranty,
            "#DOCUMENT#" to VaultsLoc.tagDocument,
            "#RECEIPT#" to VaultsLoc.tagReceipt,
            "#INVOICE#" to VaultsLoc.tagInvoice,
            "#AGREEMENT#" to VaultsLoc.tagAgreement,
            "#IDENTIFICATION#" to VaultsLoc.tagId,
            "#SLIP#" to VaultsLoc.tagSlip
        )
    }

    fun getFileNameAndExt(uri: Uri): Pair<String, String> {
        var name = uri.lastPathSegment ?: "document"
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        }
        val ext = if (name.contains(".")) "." + name.substringAfterLast('.') else ""
        val nameWithoutExt = if (name.contains(".")) name.substringBeforeLast('.') else name
        return nameWithoutExt to ext
    }

    val documentMimeTypes = remember {
        arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "application/zip"
        )
    }

    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
        }
        val added = uris.map { uri ->
            val (name, ext) = getFileNameAndExt(uri)
            AttachedFile(uri, name, ext)
        }
        newFiles = newFiles + added
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val added = uris.map { uri ->
            val ext = ".jpg"
            val name = "photo_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + "_" + UUID.randomUUID().toString().take(4)
            AttachedFile(uri, name, ext)
        }
        newFiles = newFiles + added
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            val name = "camera_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            newFiles = newFiles + AttachedFile(cameraImageUri!!, name, ".jpg")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberCustomDatePickerState(initialSelectedDateMillis = docDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { docDateMs = it }
                    showDatePicker = false
                }) { Text(Loc.General.select) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(Loc.General.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Vaults.editDoc
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isUploading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = Loc.Vaults.uploadingDoc,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = sanitizeFileName(it) },
                    label = { Text(Loc.Vaults.docName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Existing + New Attached Files
                if (existingFiles.isNotEmpty() || newFiles.isNotEmpty()) {
                    Text(
                        text = Loc.Vaults.document,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Existing files
                    existingFiles.forEachIndexed { index, file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isImage = file.extension.lowercase() in listOf(".jpg", ".jpeg", ".png", ".webp")
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isImage) Icons.Default.Image else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = file.extension.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = file.fileName.removeSuffix(file.extension),
                                onValueChange = { newName ->
                                    val sanitized = sanitizeFileName(newName)
                                    val newList = existingFiles.toMutableList()
                                    newList[index] = file.copy(fileName = sanitized)
                                    existingFiles = newList
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                trailingIcon = {
                                    Text(
                                        text = file.extension.lowercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )

                            IconButton(
                                onClick = {
                                    removedDriveIds = removedDriveIds + file.driveFileId
                                    val newList = existingFiles.toMutableList()
                                    newList.removeAt(index)
                                    existingFiles = newList
                                },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // New files
                    newFiles.forEachIndexed { index, file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isImage = file.extension.lowercase() in listOf(".jpg", ".jpeg", ".png", ".webp")
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isImage) Icons.Default.Image else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = file.extension.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = file.fileName.removeSuffix(file.extension),
                                onValueChange = { newName ->
                                    val sanitized = sanitizeFileName(newName)
                                    val newList = newFiles.toMutableList()
                                    newList[index] = file.copy(fileName = sanitized)
                                    newFiles = newList
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                trailingIcon = {
                                    Text(
                                        text = file.extension.lowercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )

                            IconButton(
                                onClick = {
                                    val newList = newFiles.toMutableList()
                                    newList.removeAt(index)
                                    newFiles = newList
                                },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Add File Buttons
                Text(
                    text = VaultsLoc.addAnother,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { docLauncher.launch(documentMimeTypes) },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentPadding = PaddingValues(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(Loc.Vaults.selectDocument, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                        }
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentPadding = PaddingValues(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(Loc.Vaults.selectPhoto, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val tempFile = File(context.externalCacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
                            val tempUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                            cameraImageUri = tempUri
                            cameraLauncher.launch(tempUri)
                        },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentPadding = PaddingValues(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(Loc.Vaults.takePhoto, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                // Tag Selector
                var isTagFocused by remember { mutableStateOf(false) }

                val filteredTags = remember(tagInput, tagsMap) {
                    if (tagInput.isBlank()) {
                        tagsMap.toList()
                    } else {
                        val lower = tagInput.lowercase()
                        tagsMap.toList().filter { it.second.lowercase().contains(lower) }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredTags.isNotEmpty()) {
                        Text(
                            text = Loc.Vaults.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredTags, key = { it.first }) { (key, label) ->
                                val isSelected = tag == key || tagInput.equals(label, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        tag = key
                                        tagInput = label
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { input ->
                            tagInput = input
                            val matchedKey = tagsMap.entries.firstOrNull { it.value.equals(input.trim(), ignoreCase = true) }?.key
                            tag = matchedKey ?: input.trim()
                        },
                        label = { Text(Loc.Vaults.tag) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isTagFocused = it.isFocused },
                        singleLine = true
                    )
                }

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${Loc.Vaults.docDate}: ${dateFormat.format(Date(docDateMs))}")
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Loc.Vaults.description) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Button(
                    onClick = {
                        if (docName.isNotBlank() && (existingFiles.isNotEmpty() || newFiles.isNotEmpty())) {
                            val updatedDoc = doc.copy(
                                name = docName.trim(),
                                tag = tag.trim(),
                                documentTimestamp = docDateMs,
                                description = description.trim(),
                                driveFileIds = existingFiles.map { it.driveFileId },
                                driveUrls = existingFiles.map { it.driveUrl },
                                fileNames = existingFiles.map { "${it.fileName}${it.extension}" },
                                fileTypes = existingFiles.map { it.fileType }
                            )
                            onConfirm(updatedDoc, newFiles, removedDriveIds)
                        }
                    },
                    enabled = docName.isNotBlank() && (existingFiles.isNotEmpty() || newFiles.isNotEmpty()) && tag.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(Loc.General.save, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
