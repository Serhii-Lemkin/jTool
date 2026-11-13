package com.jtool.android.features.vaults.components.edit_doc_sheet

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            val nameWithoutExt = if (ext.isNotEmpty()) fullName.removeSuffix(ext) else fullName
            ExistingFileItem(
                driveFileId = doc.driveFileIds[i],
                driveUrl = doc.driveUrls.getOrElse(i) { "" },
                fileName = nameWithoutExt.removeSuffix(ext),
                extension = ext,
                fileType = doc.fileTypes.getOrElse(i) { doc.fileType }
            )
        }.filter { it.driveFileId.isNotEmpty() }
    } else if (doc.driveFileId.isNotEmpty()) {
        val ext = if (doc.name.contains('.')) ".${doc.name.substringAfterLast('.')}" else ""
        val nameWithoutExt = if (ext.isNotEmpty()) doc.name.removeSuffix(ext) else doc.name
        return listOf(
            ExistingFileItem(
                driveFileId = doc.driveFileId,
                driveUrl = doc.driveUrl,
                fileName = nameWithoutExt.removeSuffix(ext),
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
    canEditFiles: Boolean = true,
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
            "#DOCUMENT#" to VaultsLoc.tagDocument,
            "#WARRANTY#" to VaultsLoc.tagWarranty,
            "#RECEIPT#" to VaultsLoc.tagReceipt,
            "#INVOICE#" to VaultsLoc.tagInvoice,
            "#AGREEMENT#" to VaultsLoc.tagAgreement,
            "#IDENTIFICATION#" to VaultsLoc.tagId,
            "#SLIP#" to VaultsLoc.tagSlip,
            "#LICENSE#" to VaultsLoc.tagLicense,
            "#CONTRACT#" to VaultsLoc.tagContract,
            "#INSURANCE#" to VaultsLoc.tagInsurance,
            "#MEDICAL#" to VaultsLoc.tagMedical,
            "#TAX#" to VaultsLoc.tagTax,
            "#VEHICLE#" to VaultsLoc.tagVehicle,
            "#PROPERTY#" to VaultsLoc.tagProperty,
            "#FINANCIAL#" to VaultsLoc.tagFinancial,
            "#CERTIFICATE#" to VaultsLoc.tagCertificate,
            "#EDUCATION#" to VaultsLoc.tagEducation
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

                if (canEditFiles) {
                    EditDocFilesSection(
                        existingFiles = existingFiles,
                        newFiles = newFiles,
                        sanitizeFileName = ::sanitizeFileName,
                        onExistingFileNameChange = { index, sanitized ->
                            val newList = existingFiles.toMutableList()
                            newList[index] = existingFiles[index].copy(fileName = sanitized)
                            existingFiles = newList
                        },
                        onRemoveExistingFile = { index ->
                            removedDriveIds = removedDriveIds + existingFiles[index].driveFileId
                            val newList = existingFiles.toMutableList()
                            newList.removeAt(index)
                            existingFiles = newList
                        },
                        onNewFileNameChange = { index, sanitized ->
                            val newList = newFiles.toMutableList()
                            newList[index] = newFiles[index].copy(fileName = sanitized)
                            newFiles = newList
                        },
                        onRemoveNewFile = { index ->
                            val newList = newFiles.toMutableList()
                            newList.removeAt(index)
                            newFiles = newList
                        },
                        onPickDocuments = { docLauncher.launch(documentMimeTypes) },
                        onPickPhotos = { galleryLauncher.launch("image/*") },
                        onTakePhoto = {
                            val tempFile = File(context.externalCacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
                            val tempUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                            cameraImageUri = tempUri
                            cameraLauncher.launch(tempUri)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                }

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

                val dateFormat = remember(context) { com.jtool.android.core.utils.getPreferredSimpleDateFormat(context) }
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
                        val hasValidFiles = !canEditFiles || existingFiles.isNotEmpty() || newFiles.isNotEmpty()
                        if (docName.isNotBlank() && hasValidFiles) {
                            val updatedDoc = if (canEditFiles) {
                                doc.copy(
                                    name = docName.trim(),
                                    tag = tag.trim(),
                                    documentTimestamp = docDateMs,
                                    description = description.trim(),
                                    driveFileIds = existingFiles.map { it.driveFileId },
                                    driveUrls = existingFiles.map { it.driveUrl },
                                    fileNames = existingFiles.map {
                                        val clean = it.fileName.removeSuffix(it.extension)
                                        "$clean${it.extension}"
                                    },
                                    fileTypes = existingFiles.map { it.fileType }
                                )
                            } else {
                                doc.copy(
                                    name = docName.trim(),
                                    tag = tag.trim(),
                                    documentTimestamp = docDateMs,
                                    description = description.trim()
                                )
                            }
                            onConfirm(
                                updatedDoc,
                                if (canEditFiles) newFiles else emptyList(),
                                if (canEditFiles) removedDriveIds else emptyList()
                            )
                            onDismiss()
                        }
                    },
                    enabled = docName.isNotBlank() && (!canEditFiles || existingFiles.isNotEmpty() || newFiles.isNotEmpty()) && tag.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(Loc.General.save, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
