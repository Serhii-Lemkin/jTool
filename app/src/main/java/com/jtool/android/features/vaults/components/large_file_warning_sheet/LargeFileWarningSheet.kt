package com.jtool.android.features.vaults.components.large_file_warning_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.vaults.AttachedFile
import com.jtool.android.features.vaults.common.VaultDocUtils
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun LargeFileWarningSheet(
    largeFiles: List<Pair<AttachedFile, Long>>,
    saveOffline: Boolean,
    maxFileSizeMb: Int = 100,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(52.dp)
            )

            Text(
                text = "Large Files Warning",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            val context = androidx.compose.ui.platform.LocalContext.current
            val isWifi = remember { VaultDocUtils.isWifiConnected(context) }

            val warningText = remember(largeFiles, saveOffline, isWifi, maxFileSizeMb) {
                val dataText = if (isWifi) "" else " mobile data and"
                if (largeFiles.size == 1) {
                    val (file, size) = largeFiles.first()
                    val formattedSize = VaultDocUtils.formatFileSize(size)
                    if (saveOffline) {
                        "File '${file.fileName}' ($formattedSize) is larger than $maxFileSizeMb MB. Uploading and caching it locally will consume a lot of$dataText device storage space."
                    } else {
                        "File '${file.fileName}' ($formattedSize) is larger than $maxFileSizeMb MB. Uploading it will consume a lot of$dataText storage space."
                    }
                } else {
                    if (saveOffline) {
                        "The following ${largeFiles.size} selected files are larger than $maxFileSizeMb MB. Uploading and caching them locally will consume a significant amount of$dataText device storage space:"
                    } else {
                        "The following ${largeFiles.size} selected files are larger than $maxFileSizeMb MB. Uploading them will consume a significant amount of$dataText storage space:"
                    }
                }
            }

            Text(
                text = warningText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (largeFiles.size > 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    largeFiles.forEach { (file, size) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${file.fileName}${file.extension}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = VaultDocUtils.formatFileSize(size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Proceed", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
