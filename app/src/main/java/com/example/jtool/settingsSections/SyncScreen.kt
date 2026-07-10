package com.example.jtool.settingsSections

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jtool.BackupManager
import com.example.jtool.Loc
import com.example.jtool.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun SyncScreen(
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    linkedUri: String?,
    onUriChanged: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var syncIntervalText by remember { mutableStateOf(settingsManager.getSyncInterval().toString()) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, flags)
                    settingsManager.saveSyncUri(it.toString())
                    onUriChanged(it.toString())
                    scope.launch { snackbarHostState.showSnackbar("Folder linked!") }
                } catch (e: Exception) {
                    scope.launch { snackbarHostState.showSnackbar("Permission denied") }
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize().systemBarsPadding()
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(Loc.syncTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Loc.cloudFolder, fontWeight = FontWeight.Bold)
                    Text(Loc.selectFolderDesc, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (linkedUri.isNullOrEmpty()) {
                        Button(onClick = { folderLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Folder, null)
                            Spacer(Modifier.width(8.dp))
                            Text(Loc.linkSyncFolder)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { 
                                scope.launch { if (backupManager.syncPush(linkedUri)) snackbarHostState.showSnackbar(Loc.t("Pushed!", "Відправлено!")) }
                            }, modifier = Modifier.weight(1f)) { Text(Loc.pushData) }
                            Button(onClick = { 
                                scope.launch { if (backupManager.syncPull(linkedUri)) snackbarHostState.showSnackbar(Loc.t("Pulled!", "Отримано!")) }
                            }, modifier = Modifier.weight(1f)) { Text(Loc.pullData) }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(linkedUri)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar(Loc.t("Could not open Drive", "Не вдалося відкрити Drive")) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, null)
                            Spacer(Modifier.width(8.dp))
                            Text(Loc.openInDrive)
                        }

                        TextButton(onClick = { settingsManager.saveSyncUri(null); onUriChanged(null) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(Loc.unlinkFolder, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Loc.syncInterval, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = syncIntervalText,
                        onValueChange = { 
                            syncIntervalText = it
                            it.toIntOrNull()?.let { seconds ->
                                if (seconds >= 5) {
                                    settingsManager.setSyncInterval(seconds)
                                }
                            }
                        },
                        label = { Text(Loc.t("Seconds (min 5)", "Секунди (мін 5)")) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }
    }
}
