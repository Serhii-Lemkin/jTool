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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            
            // Robust version extraction
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
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Loc.appUpdates, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Loc.versionInfo, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
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
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (availableUpdateVersion != null && availableUpdateVersion!! > currentVersion) {
                    Button(
                        onClick = { 
                            if (updateUrl != null) {
                                // Append filename to folder URL if not already present
                                val finalUrl = if (updateUrl!!.endsWith(".apk")) updateUrl!! 
                                              else updateUrl!!.removeSuffix("/") + "/app-release.apk"
                                
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
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
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
