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
import com.jtool.android.core.models.feature.links.LinkItem
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleActionIcon
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
    dataManager: DataManager,
    onBack: () -> Unit,
    viewModel: LinksViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                LinksViewModel(manager, settingsManager, dataManager)
            }
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var isSearchVisible by remember { mutableStateOf(false) }
    var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }
    
    LaunchedEffect(uiState.searchQuery) { draftSearchQuery = uiState.searchQuery }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                GenericTitle(
                    title = Loc.links_links, 
                    onBack = onBack,
                    isLoading = uiState.isLoading,
                    onRefresh = { viewModel.onEvent(LinksEvent.Refresh) },
                    isSearchVisible = isSearchVisible,
                    searchQuery = draftSearchQuery,
                    onSearchQueryChange = { draftSearchQuery = it },
                    onSearchClose = {
                        isSearchVisible = false
                        viewModel.onEvent(LinksEvent.UpdateSearchQuery(""))
                    },
                    onSearchTriggered = {
                        viewModel.onEvent(LinksEvent.UpdateSearchQuery(draftSearchQuery))
                    },
                    searchPlaceholder = Loc.links_searchLinks,
                    actions = {
                        TitleActionIcon(icon = Icons.Default.Search, onClick = { isSearchVisible = true })
                    }
                )
            },
            modifier = Modifier.fillMaxSize().imePadding()
        ) { padding ->
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.onEvent(LinksEvent.Refresh) },
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(uiState.filteredLinks, key = { it.id }) { link ->
                            var itemMenuExpanded by remember { mutableStateOf(false) }

                            ActionListItem(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, Loc.links_invalidUrl, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                headline = link.title,
                                supportingText = link.url,
                                metadataText = "${Loc.general_by} ${uiState.userNames[link.lastUpdatedBy] ?: link.lastUpdatedBy}",
                                trailingContent = {
                                    Box {
                                        IconButton(
                                            onClick = { itemMenuExpanded = true },
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = null)
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
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        }
                        
                        item {
                            Spacer(Modifier.height(100.dp))
                        }
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
