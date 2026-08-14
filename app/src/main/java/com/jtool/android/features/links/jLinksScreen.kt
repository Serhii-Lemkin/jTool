package com.jtool.android.features.links

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.models.LinkItem
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
fun JLinksScreen(
    manager: JLinksManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
    viewModel: LinksViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                LinksViewModel(manager, settingsManager, groupSyncManager)
            }
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = Loc.links,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { viewModel.onEvent(LinksEvent.ToggleMenu(true)) }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(
                    expanded = uiState.menuExpanded,
                    onDismissRequest = { viewModel.onEvent(LinksEvent.ToggleMenu(false)) }
                ) {
                    DropdownMenuItem(
                        text = { Text(Loc.addLink) },
                        onClick = { 
                            viewModel.onEvent(LinksEvent.ToggleAddDialog(true))
                            viewModel.onEvent(LinksEvent.ToggleMenu(false))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onEvent(LinksEvent.UpdateSearchQuery(it)) },
            label = { Text(Loc.searchLinks) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onEvent(LinksEvent.UpdateSearchQuery("")) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(uiState.filteredLinks, key = { it.id }) { link ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = link.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp)
                                Text(text = link.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(link.url))
                                Toast.makeText(context, Loc.t("Copied!", "Скопійовано!"), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, Loc.t("Invalid URL", "Невірна адреса"), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                viewModel.onEvent(LinksEvent.RequestDelete(link))
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = "${Loc.by} ${uiState.userNames[link.lastUpdatedBy] ?: link.lastUpdatedBy}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.End).padding(end = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(LinksEvent.ToggleAddDialog(false)) },
            title = { Text(Loc.addLink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.newTitle,
                        onValueChange = { viewModel.onEvent(LinksEvent.UpdateNewTitle(it)) },
                        label = { Text(Loc.linkTitle) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.newUrl,
                        onValueChange = { viewModel.onEvent(LinksEvent.UpdateNewUrl(it)) },
                        label = { Text(Loc.linkUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("https://...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(LinksEvent.AddLink)
                    },
                    enabled = uiState.newTitle.isNotBlank() && uiState.newUrl.isNotBlank()
                ) {
                    Text(Loc.add)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(LinksEvent.ToggleAddDialog(false)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    if (uiState.linkToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(LinksEvent.RequestDelete(null)) },
            title = { Text(Loc.confirmDeleteTitle) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(LinksEvent.ConfirmDelete)
                }) {
                    Text(Loc.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(LinksEvent.RequestDelete(null)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun LinksPreview() {
    val context = LocalContext.current
    MaterialTheme {
        JLinksScreen(
            manager = JLinksManager(context),
            backupManager = BackupManager(context),
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context)
        )
    }
}
