package com.jtool.android.features.vaults.components.upload_doc_sheet

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
import com.jtool.android.core.models.vaults.Vault
import com.jtool.android.features.vaults.AttachedFile
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.features.vaults.components.large_file_warning_sheet.LargeFileWarningSheet
import com.jtool.android.ui.components.common.GenericSheet
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocSheet(
    vault: Vault,
    isUploading: Boolean = false,
    onDismiss: () -> Unit,
    onUpload: (List<AttachedFile>, String, String, Long, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val isLargeWarningEnabled = remember { settingsManager.isLargeFileWarningEnabled() }
    val maxFileSizeMb = remember { settingsManager.getMaxFileSizeWithoutWarningMb() }

    var attachedFiles by remember { mutableStateOf<List<AttachedFile>>(emptyList()) }
    var recordName by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var docDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var description by remember { mutableStateOf("") }
    var saveOffline by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var largeFilesToWarn by remember { mutableStateOf<List<Pair<AttachedFile, Long>>?>(null) }

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

    // Default tag logic
    LaunchedEffect(Unit) {
        if (tag.isEmpty()) tag = "#DOCUMENT#"
    }

    LaunchedEffect(attachedFiles) {
        if (recordName.isBlank() && attachedFiles.isNotEmpty()) {
            recordName = sanitizeFileName(attachedFiles.first().fileName)
        }
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
        val newFiles = uris.map { uri ->
            val (name, ext) = getFileNameAndExt(uri)
            AttachedFile(uri, name, ext)
        }
        attachedFiles = attachedFiles + newFiles
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val newFiles = uris.map { uri ->
            val ext = ".jpg"
            val name = "photo_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + "_" + UUID.randomUUID().toString().take(4)
            AttachedFile(uri, name, ext)
        }
        attachedFiles = attachedFiles + newFiles
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            val name = "camera_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            attachedFiles = attachedFiles + AttachedFile(cameraImageUri!!, name, ".jpg")
        }
    }

    if (largeFilesToWarn != null) {
        LargeFileWarningSheet(
            largeFiles = largeFilesToWarn!!,
            saveOffline = saveOffline,
            maxFileSizeMb = maxFileSizeMb,
            onConfirm = {
                val filesToUpload = attachedFiles
                val nameToUpload = recordName.trim()
                largeFilesToWarn = null
                onUpload(filesToUpload, nameToUpload, tag, docDateMs, description.trim(), saveOffline)
            },
            onDismiss = { largeFilesToWarn = null }
        )
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
        title = Loc.Vaults.uploadDocument
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
                
                // Files List
                if (attachedFiles.isNotEmpty()) {
                    OutlinedTextField(
                        value = recordName,
                        onValueChange = { recordName = sanitizeFileName(it) },
                        label = { Text(Loc.Vaults.docName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = Loc.Vaults.document,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    attachedFiles.forEachIndexed { index, file ->
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
                            
                            val fileSize = VaultDocUtils.getFileSizeFromUri(context, file.uri)
                            
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = file.fileName.removeSuffix(file.extension),
                                    onValueChange = { newName ->
                                        val sanitized = sanitizeFileName(newName)
                                        val newList = attachedFiles.toMutableList()
                                        newList[index] = file.copy(fileName = sanitized)
                                        attachedFiles = newList
                                    },
                                    modifier = Modifier.fillMaxWidth(),
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
                                if (fileSize > 0) {
                                    Text(
                                        text = VaultDocUtils.formatFileSize(fileSize),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = {
                                    val newList = attachedFiles.toMutableList()
                                    newList.removeAt(index)
                                    attachedFiles = newList
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
                    text = if (attachedFiles.isEmpty()) Loc.Vaults.selectFile else VaultsLoc.addAnother,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = if (attachedFiles.isNotEmpty()) 8.dp else 0.dp)
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
                            Icon(if (attachedFiles.isEmpty()) Icons.Default.Description else Icons.Default.Add, null, modifier = Modifier.size(28.dp))
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
                            Icon(if (attachedFiles.isEmpty()) Icons.Default.PhotoLibrary else Icons.Default.Add, null, modifier = Modifier.size(28.dp))
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
                            Icon(if (attachedFiles.isEmpty()) Icons.Default.CameraAlt else Icons.Default.Add, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(Loc.Vaults.takePhoto, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (attachedFiles.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    // Tag Selector
                    var tagInput by remember { mutableStateOf("") }
                    var isTagFocused by remember { mutableStateOf(false) }

                    LaunchedEffect(tag) {
                        if (tag.isNotEmpty()) {
                            tagInput = tagsMap[tag] ?: tag
                        }
                    }

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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = VaultsLoc.saveOfflineCache,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = saveOffline,
                            onCheckedChange = { saveOffline = it }
                        )
                    }

                    Button(
                        onClick = {
                            if (attachedFiles.isNotEmpty() && recordName.isNotBlank()) {
                                val maxSizeBytes = maxFileSizeMb.toLong() * 1024L * 1024L
                                val largeFiles = if (isLargeWarningEnabled) {
                                    attachedFiles.map { f ->
                                        val size = VaultDocUtils.getFileSizeFromUri(context, f.uri)
                                        f to size
                                    }.filter { it.second > maxSizeBytes }
                                } else emptyList()

                                if (largeFiles.isNotEmpty()) {
                                    largeFilesToWarn = largeFiles
                                } else {
                                    onUpload(attachedFiles, recordName.trim(), tag, docDateMs, description.trim(), saveOffline)
                                }
                            }
                        },
                        enabled = attachedFiles.isNotEmpty() && recordName.isNotBlank() && tag.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(Loc.General.save, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
