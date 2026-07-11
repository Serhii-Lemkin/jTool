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
import androidx.compose.ui.unit.dp
import com.example.jtool.GroupSyncManager
import com.example.jtool.Loc
import com.example.jtool.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun RequestsScreen(
    groupSyncManager: GroupSyncManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var requestText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Loc.requestsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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
                        groupSyncManager.pushSuggestion(
                            requestText, 
                            settingsManager.getAppUserId()
                        )
                        requestText = ""
                        snackbarHostState.showSnackbar(Loc.requestSent)
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

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(Loc.t("Troubleshooting", "Вирішення проблем"), style = MaterialTheme.typography.titleSmall)
                Text(
                    Loc.autoBlockerNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = {
                        try {
                            val intent = Intent("android.settings.SECURITY_SETTINGS")
                            context.startActivity(intent)
                        } catch (e: Exception) {
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
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
