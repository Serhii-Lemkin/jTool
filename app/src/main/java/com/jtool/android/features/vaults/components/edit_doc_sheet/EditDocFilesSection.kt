package com.jtool.android.features.vaults.components.edit_doc_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.features.vaults.AttachedFile

@Composable
fun EditDocFilesSection(
    existingFiles: List<ExistingFileItem>,
    newFiles: List<AttachedFile>,
    sanitizeFileName: (String) -> String,
    onExistingFileNameChange: (Int, String) -> Unit,
    onRemoveExistingFile: (Int) -> Unit,
    onNewFileNameChange: (Int, String) -> Unit,
    onRemoveNewFile: (Int) -> Unit,
    onPickDocuments: () -> Unit,
    onPickPhotos: () -> Unit,
    onTakePhoto: () -> Unit
) {
    if (existingFiles.isNotEmpty() || newFiles.isNotEmpty()) {
        Text(
            text = Loc.Vaults.document,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        existingFiles.forEachIndexed { index, file ->
            EditFileNameRow(
                name = file.fileName,
                extension = file.extension,
                onNameChange = { onExistingFileNameChange(index, sanitizeFileName(it).removeSuffix(file.extension)) },
                onRemove = { onRemoveExistingFile(index) }
            )
        }

        newFiles.forEachIndexed { index, file ->
            EditFileNameRow(
                name = file.fileName,
                extension = file.extension,
                onNameChange = { onNewFileNameChange(index, sanitizeFileName(it).removeSuffix(file.extension)) },
                onRemove = { onRemoveNewFile(index) }
            )
        }
    }

    Text(
        text = VaultsLoc.addAnother,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AddFileOptionButton(
            icon = Icons.Default.Add,
            label = Loc.Vaults.selectDocument,
            onClick = onPickDocuments,
            modifier = Modifier.weight(1f)
        )
        AddFileOptionButton(
            icon = Icons.Default.PhotoLibrary,
            label = Loc.Vaults.selectPhoto,
            onClick = onPickPhotos,
            modifier = Modifier.weight(1f)
        )
        AddFileOptionButton(
            icon = Icons.Default.CameraAlt,
            label = Loc.Vaults.takePhoto,
            onClick = onTakePhoto,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditFileNameRow(
    name: String,
    extension: String,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val isImage = extension.lowercase() in listOf(".jpg", ".jpeg", ".png", ".webp")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                text = extension.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = name.removeSuffix(extension),
            onValueChange = onNameChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            trailingIcon = {
                Text(
                    text = extension.lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AddFileOptionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        }
    }
}
