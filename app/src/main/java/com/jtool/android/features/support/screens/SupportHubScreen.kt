package com.jtool.android.features.support.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.features.support.components.*
import com.jtool.android.features.support.logic.*
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleDropdownMenu
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.components.comments.CommentEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportHubScreen(
    syncManager: GroupSyncManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    viewModel: SupportViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SupportViewModel(syncManager, settingsManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            android.widget.Toast.makeText(context, Loc.support_sent_success, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.onEvent(SupportEvent.ResetStatus)
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.onEvent(SupportEvent.ResetStatus)
        }
    }

    val globalUserNames by syncManager.userNamesFlow().collectAsState(initial = emptyMap())

    val filteredRequests = remember(uiState.requests, uiState.filterType, uiState.showArchived, uiState.searchQuery) {
        uiState.requests
            .filter { if (uiState.filterType == null) true else it.type == uiState.filterType }
            .filter { 
                if (uiState.showArchived) {
                    it.status == SupportStatus.CLOSED || it.status == SupportStatus.CUT
                } else {
                    it.status == SupportStatus.OPENED || it.status == SupportStatus.IN_PROGRESS
                }
            }
            .filter {
                if (uiState.searchQuery.isBlank()) true
                else {
                    it.number.toString().contains(uiState.searchQuery) || 
                    it.text.contains(uiState.searchQuery, ignoreCase = true)
                }
            }
            .sortedByDescending { it.timestamp }
    }
    
    val pagedRequests = remember(filteredRequests, uiState.pagingLimit) {
        filteredRequests.take(uiState.pagingLimit)
    }

    BackHandler {
        if (uiState.currentSubScreen != SupportScreenType.HUB) {
            viewModel.onEvent(SupportEvent.Navigate(SupportScreenType.HUB))
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isSearchVisible && uiState.currentSubScreen == SupportScreenType.HUB) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onEvent(SupportEvent.UpdateSearchQuery(it)) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            placeholder = { Text(Loc.general_search) },
                            trailingIcon = {
                                IconButton(onClick = { viewModel.onEvent(SupportEvent.ToggleSearch) }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                )
            } else {
                val appUserId = viewModel.getAppUserId()
                val authUid = viewModel.getAuthUserId()
                GenericTitle(
                    title = when (uiState.currentSubScreen) {
                        SupportScreenType.HUB -> Loc.support_hub_title
                        SupportScreenType.DETAIL -> Loc.support_request_number.format(uiState.selectedRequest?.number ?: 0)
                        SupportScreenType.NEW_FORM -> Loc.support_new_request
                    },
                    onBack = {
                        if (uiState.currentSubScreen != SupportScreenType.HUB) {
                            viewModel.onEvent(SupportEvent.Navigate(SupportScreenType.HUB))
                        } else {
                            onBack()
                        }
                    },
                    actions = {
                        if (uiState.currentSubScreen == SupportScreenType.HUB) {
                            TitleActionIcon(icon = Icons.Default.Search, onClick = { viewModel.onEvent(SupportEvent.ToggleSearch) })
                            
                            TitleDropdownMenu(icon = Icons.Default.FilterList) {
                                DropdownMenuItem(
                                    text = { Text(if (uiState.showArchived) "Show Active" else Loc.support_show_archived) },
                                    onClick = { 
                                        viewModel.onEvent(SupportEvent.ToggleArchived(!uiState.showArchived))
                                    },
                                    leadingIcon = { Icon(Icons.Default.Archive, null) },
                                    trailingIcon = { if (uiState.showArchived) Icon(Icons.Default.Check, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("All Types") },
                                    onClick = { viewModel.onEvent(SupportEvent.SetFilterType(null)) },
                                    trailingIcon = { if (uiState.filterType == null) Icon(Icons.Default.Check, null) }
                                )
                                SupportType.entries.filter { it != SupportType.CRASH }.forEach { type ->
                                    DropdownMenuItem(
                                        text = { 
                                            val name = when(type) {
                                                SupportType.BUG -> Loc.support_type_bug
                                                SupportType.FEATURE -> Loc.support_type_feature
                                                SupportType.CHANGE -> Loc.support_type_change
                                                else -> type.name
                                            }
                                            Text(name)
                                        },
                                        onClick = { viewModel.onEvent(SupportEvent.SetFilterType(type)) },
                                        trailingIcon = { if (uiState.filterType == type) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (uiState.currentSubScreen == SupportScreenType.HUB) {
                UnifiedFAB(
                    onClick = {},
                    dropdownContent = { onDismiss ->
                        SupportType.entries.filter { it != SupportType.CRASH }.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            SupportType.BUG -> Loc.support_type_bug
                                            SupportType.FEATURE -> Loc.support_type_feature
                                            SupportType.CHANGE -> Loc.support_type_change
                                            else -> ""
                                        }
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    viewModel.onEvent(SupportEvent.SetFilterType(type))
                                    viewModel.onEvent(SupportEvent.Navigate(SupportScreenType.NEW_FORM))
                                }
                            )
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize().navigationBarsPadding()
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState.currentSubScreen) {
                SupportScreenType.HUB -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (uiState.isLoading && filteredRequests.isNotEmpty()) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        }

                        if (filteredRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator()
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = Loc.support_welcome_title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = Loc.support_welcome_msg,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(pagedRequests, key = { it.id }) { request ->
                                    SupportListItem(
                                        request = request,
                                        onClick = { viewModel.onEvent(SupportEvent.OpenRequest(request)) }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                }
                                
                                if (filteredRequests.size > uiState.pagingLimit) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            TextButton(onClick = { viewModel.onEvent(SupportEvent.LoadMore) }) {
                                                Text("Load More")
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(100.dp))
                                }
                            }
                        }
                    }
                }
                SupportScreenType.DETAIL -> {
                    uiState.selectedRequest?.let { request ->
                        val appUserId = viewModel.getAppUserId()
                        val authUid = viewModel.getAuthUserId()
                        
                        SupportDetailCard(
                            request = request,
                            isAdmin = uiState.isAdmin,
                            currentUserId = appUserId,
                            onStatusUpdate = { viewModel.onEvent(SupportEvent.UpdateStatus(request.id, it)) },
                            onTypeUpdate = { viewModel.onEvent(SupportEvent.UpdateType(request.id, it)) },
                            onDelete = { viewModel.onEvent(SupportEvent.DeleteRequest(request.id)) },
                            onCommentEvent = { event ->
                                when (event) {
                                    is CommentEvent.Add -> viewModel.onEvent(SupportEvent.AddComment(request.id, event.content, event.parentId))
                                    is CommentEvent.Edit -> viewModel.onEvent(SupportEvent.EditComment(request.id, event.commentId, event.newContent))
                                    is CommentEvent.Delete -> viewModel.onEvent(SupportEvent.DeleteComment(request.id, event.commentId))
                                }
                            },
                            userNames = globalUserNames,
                            isOwner = { it.userId == appUserId || it.userId == authUid }
                        )
                    }
                }
                SupportScreenType.NEW_FORM -> {
                    SupportNewRequestForm(
                        text = uiState.requestText,
                        onTextChange = { viewModel.onEvent(SupportEvent.UpdateRequestText(it)) },
                        isSending = uiState.isSending,
                        selectedType = uiState.filterType ?: SupportType.BUG,
                        onSubmit = { viewModel.onEvent(SupportEvent.SubmitRequest(uiState.filterType ?: SupportType.BUG, context)) }
                    )
                }
            }
        }
    }
}
