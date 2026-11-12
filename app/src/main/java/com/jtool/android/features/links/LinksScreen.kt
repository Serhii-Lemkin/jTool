package com.jtool.android.features.links

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.models.feature.links.LinkItem
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.LinksLoc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.components.layout.TitleDropdownMenu
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.features.links.components.link_delete_confirm_sheet.LinkDeleteConfirmSheet
import com.jtool.android.features.links.components.link_edit_sheet.LinkEditSheet
import com.jtool.android.features.links.components.link_filter_sheet.LinkFilterSheet
import com.jtool.android.features.links.components.link_action_sheet.LinkActionSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType
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
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }

    var isSearchVisible by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var selectedLinkForAction by remember { mutableStateOf<LinkItem?>(null) }
    
    val activeDbTagKeys = remember(uiState.links) {
        uiState.links.map { it.tag.ifBlank { "#COMMON#" }.uppercase() }.toSet()
    }

    val availableTags = remember(activeDbTagKeys) {
        val std = LinksLoc.allStandardLinkTags.filter { activeDbTagKeys.contains(it.first) }
        if (std.isEmpty()) {
            listOf("#COMMON#" to LinksLoc.tagCommon)
        } else {
            std
        }
    }

    val hasActiveTagFilter = remember(uiState.selectedTags, availableTags) {
        uiState.selectedTags.isNotEmpty() && uiState.selectedTags.size < availableTags.size
    }

    LaunchedEffect(uiState.searchQuery) { draftSearchQuery = uiState.searchQuery }

    BaseScreen(
        title = Loc.Links.links,
        isLoading = uiState.isLoading,
        onBack = onBack,
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
        searchPlaceholder = Loc.Links.searchLinks,
        actions = {
            TitleActionIcon(icon = Icons.Default.Search, onClick = { isSearchVisible = true })
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (hasActiveTagFilter) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                if (hasActiveTagFilter) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
            }
            TitleDropdownMenu {
                ActionListItem(
                    headline = Loc.General.learnMore,
                    supportingText = Loc.s("links_learnMoreSubtitle", "View online guide and feature documentation"),
                    leadingContent = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, null, "personal-hub")
                    }
                )
            }
        },
        floatingActionButton = {
            UnifiedFAB(
                onClick = { viewModel.onEvent(LinksEvent.ToggleAddDialog(true)) },
                contentDescription = Loc.Links.addLink
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading || uiState.isRefreshing,
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
                        val localizedTag = LinksLoc.getLocalizedLinkTag(link.tag)
                        val uName = uiState.userNames[link.lastUpdatedBy] ?: link.lastUpdatedBy
                        val meta = "$localizedTag • ${Loc.General.by} $uName"

                        ActionListItem(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    infoMessage = Loc.Links.invalidUrl to MessageType.ERROR
                                }
                            },
                            headline = link.title,
                            supportingText = link.url,
                            supportingMaxLines = 1,
                            metadataText = meta,
                            trailingContent = {
                                IconButton(
                                    onClick = { selectedLinkForAction = link },
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
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

    if (showFilterSheet) {
        LinkFilterSheet(
            allTags = availableTags,
            selectedTags = uiState.selectedTags,
            onApplyFilters = { tags -> viewModel.onEvent(LinksEvent.SetTagFilters(tags)) },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (uiState.showAddDialog) {
        LinkEditSheet(
            sheetTitle = if (uiState.editingLink != null) Loc.General.edit else Loc.Links.addLink,
            buttonText = if (uiState.editingLink != null) Loc.General.save else Loc.General.add,
            title = uiState.newTitle,
            url = uiState.newUrl,
            tag = uiState.newTag,
            onTitleChange = { viewModel.onEvent(LinksEvent.UpdateNewTitle(it)) },
            onUrlChange = { viewModel.onEvent(LinksEvent.UpdateNewUrl(it)) },
            onTagChange = { viewModel.onEvent(LinksEvent.UpdateNewTag(it)) },
            onDismiss = { viewModel.onEvent(LinksEvent.RequestEdit(null)); viewModel.onEvent(LinksEvent.ToggleAddDialog(false)) },
            onConfirm = { viewModel.onEvent(LinksEvent.SaveLink) }
        )
    }

    if (selectedLinkForAction != null) {
        val link = selectedLinkForAction!!
        LinkActionSheet(
            onCopy = {
                clipboardManager.setText(AnnotatedString(link.url))
                infoMessage = Loc.Links.copied to MessageType.SUCCESS
                selectedLinkForAction = null
            },
            onEdit = {
                viewModel.onEvent(LinksEvent.RequestEdit(link))
                selectedLinkForAction = null
            },
            onDelete = {
                viewModel.onEvent(LinksEvent.RequestDelete(link))
                selectedLinkForAction = null
            },
            onDismiss = { selectedLinkForAction = null }
        )
    }

    if (uiState.linkToDelete != null) {
        LinkDeleteConfirmSheet(
            onDismiss = { viewModel.onEvent(LinksEvent.RequestDelete(null)) },
            onConfirm = { viewModel.onEvent(LinksEvent.ConfirmDelete) }
        )
    }

    if (infoMessage != null) {
        MessageSheet(
            message = infoMessage!!.first,
            type = infoMessage!!.second,
            onDismiss = { infoMessage = null }
        )
    }
}
