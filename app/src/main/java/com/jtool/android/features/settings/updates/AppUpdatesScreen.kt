package com.jtool.android.features.settings.updates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc

@Composable
fun AppUpdatesScreen(
    backupManager: BackupManager,
    viewModel: AppUpdatesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val context = this[APPLICATION_KEY]!!.applicationContext
                AppUpdatesViewModel(context, backupManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(Loc.Settings.versionInfo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 16.dp))
            
            val statusText = if (uiState.availableVersionCode != null && uiState.availableVersionName != null) {
                if (uiState.availableVersionCode!! > uiState.currentVersionCode) {
                    Loc.Settings.updateAvailableMsg.format(uiState.currentVersionName, uiState.availableVersionName)
                } else {
                    Loc.Settings.upToDateMsg.format(uiState.currentVersionName)
                }
            } else if (uiState.isChecking) {
                Loc.Settings.checkingUpdates
            } else {
                Loc.Settings.currentVersionLabel.format(uiState.currentVersionName)
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.availableVersionCode != null && uiState.availableVersionCode!! > uiState.currentVersionCode) {
                Button(
                    onClick = { 
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.jtool.android"))
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jtool.android"))
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SystemUpdate, null)
                    Spacer(Modifier.width(8.dp))
                    Text(Loc.Settings.updateNow)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.onEvent(AppUpdatesEvent.CheckUpdates) },
                    enabled = !uiState.isChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isChecking) Loc.Settings.checking else Loc.Settings.checkForUpdates)
                }
                
                if (uiState.availableVersionCode != null && uiState.availableVersionCode!! <= uiState.currentVersionCode) {
                    Text(
                        Loc.Settings.latestVersionMsg,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(Loc.Settings.releaseNotes, style = MaterialTheme.typography.titleSmall)
            Text(
                Loc.Settings.releaseNotesDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            OutlinedButton(
                onClick = {
                    val settingsManager = com.jtool.android.core.settings.SettingsManager(context)
                    val dataManager = com.jtool.android.core.sync.DataManager(context)
                    com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "changelog")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, null)
                Spacer(Modifier.width(8.dp))
                Text(Loc.Settings.viewChangelog)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(Loc.Settings.playStoreUpdates, style = MaterialTheme.typography.titleSmall)
            Text(
                Loc.Settings.playStoreUpdatesDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.jtool.android"))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jtool.android"))
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Loc.Settings.viewInPlayStore)
            }
        }
    }
}
