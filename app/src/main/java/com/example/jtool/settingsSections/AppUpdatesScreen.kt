package com.example.jtool.settingsSections

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.jtool.BackupManager
import com.example.jtool.Loc
import com.example.jtool.GroupSyncManager
import com.example.jtool.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun AppUpdatesScreen(
    backupManager: BackupManager,
    groupSyncManager: GroupSyncManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = backupManager.getCurrentVersionCode()
    var availableUpdateVersion by remember { mutableStateOf<Int?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf<String?>(null) }

    fun checkUpdates() {
        isChecking = true
        scope.launch {
            val appInfo = groupSyncManager.getLatestAppInfo()
            
            availableUpdateVersion = when (val v = appInfo?.get("versionCode")) {
                is Long -> v.toInt()
                is Int -> v
                is Number -> v.toInt()
                is String -> v.toIntOrNull()
                else -> null
            }

            updateUrl = appInfo?.get("downloadUrl") as? String
            
            if (updateUrl != null) {
                settingsManager.setUpdateUrl(updateUrl)
            }
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        checkUpdates()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(Loc.versionInfo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 16.dp))
            
            val statusText = if (availableUpdateVersion != null) {
                if (availableUpdateVersion!! > currentVersion) {
                    Loc.updateAvailableMsg.format(currentVersion, availableUpdateVersion)
                } else {
                    Loc.upToDateMsg.format(currentVersion)
                }
            } else if (isChecking) {
                Loc.t("Checking for updates...", "Перевірка оновлень...")
            } else {
                Loc.t("Current version: $currentVersion", "Поточна версія: $currentVersion")
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (availableUpdateVersion != null && availableUpdateVersion!! > currentVersion) {
                Button(
                    onClick = { 
                        if (updateUrl != null) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl!!))
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SystemUpdate, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.updateNow)
                }
            } else {
                OutlinedButton(
                    onClick = { checkUpdates() },
                    enabled = !isChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isChecking) Loc.t("Checking...", "Перевірка...") else Loc.checkForUpdates)
                }
                
                if (availableUpdateVersion != null && availableUpdateVersion!! <= currentVersion) {
                    Text(
                        Loc.t("You are on the latest version!", "У вас остання версія!"),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(32.dp))

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
}
