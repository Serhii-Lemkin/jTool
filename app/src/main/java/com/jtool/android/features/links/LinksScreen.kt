package com.jtool.android.features.links

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.models.LinkItem
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.UnifiedFAB
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(
    manager: LinksManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
    onBack: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                GenericTitle(title = Loc.links_links, onBack = onBack)
            },
            modifier = Modifier.fillMaxSize().imePadding()
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onEvent(LinksEvent.UpdateSearchQuery(it)) },
                    label = { Text(Loc.links_searchLinks) },
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
                        var itemMenuExpanded by remember { mutableStateOf(false) }

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
                                        Text(text = link.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = link.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, Loc.links_invalidUrl, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.size(20.dp))
                                    }

                                    Box {
                                        IconButton(
                                            onClick = { itemMenuExpanded = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(20.dp))
                                        }
                                        DropdownMenu(
                                            expanded = itemMenuExpanded,
                                            onDismissRequest = { itemMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(Loc.links_copy) },
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(link.url))
                                                    Toast.makeText(context, Loc.links_copied, Toast.LENGTH_SHORT).show()
                                                    itemMenuExpanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(Loc.general_delete) },
                                                onClick = {
                                                    viewModel.onEvent(LinksEvent.RequestDelete(link))
                                                    itemMenuExpanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${Loc.general_by} ${uiState.userNames[link.lastUpdatedBy] ?: link.lastUpdatedBy}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.align(Alignment.End).padding(end = 12.dp)
                                )
                            }
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }

        UnifiedFAB(
            onClick = { viewModel.onEvent(LinksEvent.ToggleAddDialog(true)) },
            contentDescription = Loc.links_addLink
        )
    }

    if (uiState.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(LinksEvent.ToggleAddDialog(false)) },
            title = { Text(Loc.links_addLink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.newTitle,
                        onValueChange = { viewModel.onEvent(LinksEvent.UpdateNewTitle(it)) },
                        label = { Text(Loc.links_linkTitle) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.newUrl,
                        onValueChange = { viewModel.onEvent(LinksEvent.UpdateNewUrl(it)) },
                        label = { Text(Loc.links_linkUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(Loc.links_urlHint) }
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
                    Text(Loc.general_add)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(LinksEvent.ToggleAddDialog(false)) }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }

    if (uiState.linkToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(LinksEvent.RequestDelete(null)) },
            title = { Text(Loc.links_confirmDeleteLinkTitle) },
            text = { Text(Loc.links_confirmDeleteLinkMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(LinksEvent.ConfirmDelete)
                }) {
                    Text(Loc.general_delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(LinksEvent.RequestDelete(null)) }) {
                    Text(Loc.general_cancel)
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
        LinksScreen(
            manager = LinksManager(context),
            backupManager = BackupManager(context),
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context),
            onBack = {}
        )
    }
}
