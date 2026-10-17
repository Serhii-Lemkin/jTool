package com.jtool.android.features.vaults.components.share_doc_sheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jtool.android.core.models.vaults.VaultDocument
import com.jtool.android.ui.components.common.GenericSheet
import java.io.File

@Composable
fun ShareDocSheet(
    doc: VaultDocument,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasLocalFile = doc.localPath.isNotEmpty() && File(doc.localPath).exists()

    GenericSheet(
        onDismiss = onDismiss,
        title = "Share Document"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasLocalFile) {
                OutlinedButton(
                    onClick = {
                        try {
                            val file = File(doc.localPath)
                            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share File"))
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error sharing file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share Local File Attachment", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${doc.name}\n${doc.driveUrl}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Google Drive Link"))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Link, null)
                Spacer(Modifier.width(8.dp))
                Text("Share Google Drive Link", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Drive Link", doc.driveUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null)
                Spacer(Modifier.width(8.dp))
                Text("Copy Drive Link", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
