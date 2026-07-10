package com.example.jtool.settingsSections

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jtool.BackupManager
import com.example.jtool.Loc
import com.example.jtool.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun RequestsScreen(
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    linkedUri: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var requestText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize().systemBarsPadding()
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(Loc.requestsTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                text = Loc.requestsSubtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (linkedUri.isNullOrEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))) {
                    Text(
                        Loc.linkFolderToUpdate,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                OutlinedTextField(
                    value = requestText,
                    onValueChange = { requestText = it },
                    label = { Text(Loc.enterRequest) },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    enabled = !isSending
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isSending = true
                        scope.launch {
                            val success = backupManager.sendRequest(
                                linkedUri, 
                                requestText, 
                                settingsManager.getUserName()
                            )
                            if (success) {
                                requestText = ""
                                snackbarHostState.showSnackbar(Loc.requestSent)
                            } else {
                                snackbarHostState.showSnackbar(Loc.t("Failed to send request", "Помилка при надсиланні"))
                            }
                            isSending = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending && requestText.isNotBlank()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(Loc.sendRequest)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Loc.t("Troubleshooting", "Вирішення проблем"), fontWeight = FontWeight.Bold)
                    Text(
                        Loc.autoBlockerNote,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(
                        onClick = {
                            try {
                                val intent = Intent("android.settings.SECURITY_SETTINGS")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Loc.openSecuritySettings)
                    }
                }
            }
        }
    }
}
