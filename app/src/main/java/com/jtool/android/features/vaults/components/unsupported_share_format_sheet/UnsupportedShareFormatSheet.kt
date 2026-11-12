package com.jtool.android.features.vaults.components.unsupported_share_format_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.sync.SharedUnsupportedContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnsupportedShareFormatSheet(
    content: SharedUnsupportedContent,
    onRequestSupport: (mimeType: String, details: String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Loc.s("vaults_unsupportedFormatTitle", "Unsupported Format"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = Loc.s("vaults_unsupportedFormatMsg", "This file or data format (%s) is not supported yet in jTools. Would you like to request format support in the Support Hub?").format(content.mimeType),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(Loc.General.cancel)
                }

                Button(
                    onClick = {
                        onRequestSupport(content.mimeType, content.details)
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(Loc.s("vaults_requestFormatSupport", "Request Support"), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
