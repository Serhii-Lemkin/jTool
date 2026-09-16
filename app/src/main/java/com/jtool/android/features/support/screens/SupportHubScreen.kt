package com.jtool.android.features.support.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportScreenTypeEnum
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import com.jtool.android.features.support.components.*
import com.jtool.android.features.support.components.support_filter_sheet.SupportFilterSheet
import com.jtool.android.features.support.logic.*
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleDropdownMenu
import com.jtool.android.core.components.layout.TitleActionIcon
import com.jtool.android.core.components.layout.UnifiedFAB
import com.jtool.android.core.components.comments.CommentEvent
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType

val SupportTypeEnumIcons = mapOf(
    SupportTypeEnum.BUG to Icons.Default.BugReport,
    SupportTypeEnum.FEATURE to Icons.Default.Star,
    SupportTypeEnum.CHANGE to Icons.Default.Edit,
    SupportTypeEnum.CRASH to Icons.Default.Warning
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportHubScreen(
    dataManager: DataManager,
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    viewModel: SupportViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SupportViewModel(dataManager, settingsManager)
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }
    var showTypeSheet by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }
    
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            infoMessage = Loc.Support.sent_success to MessageType.SUCCESS
            viewModel.onEvent(SupportEvent.ResetStatus)
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            infoMessage = it to MessageType.ERROR
            viewModel.onEvent(SupportEvent.ResetStatus)
        }
    }

    val globalUserNames by dataManager.userNamesFlow().collectAsState(initial = emptyMap())

    if (showFilterSheet) {
        SupportFilterSheet(
            activeTypes = uiState.activeTypeFilters,
            activeStatuses = uiState.activeStatusFilters,
            onToggleType = { viewModel.onEvent(SupportEvent.ToggleTypeFilter(it)) },
            onToggleStatus = { viewModel.onEvent(SupportEvent.ToggleStatusFilter(it)) },
            onClearAll = { viewModel.onEvent(SupportEvent.ClearFilters) },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showTypeSheet) {
        com.jtool.android.features.support.components.type_selection_sheet.SupportTypeSelectionSheet(
            onTypeSelected = { type ->
                viewModel.onEvent(SupportEvent.SetNewRequestType(type))
                viewModel.onEvent(SupportEvent.Navigate(SupportScreenTypeEnum.NEW_FORM))
            },
            onDismiss = { showTypeSheet = false }
        )
    }

    if (infoMessage != null) {
        MessageSheet(
            message = infoMessage!!.first,
            type = infoMessage!!.second,
            onDismiss = { infoMessage = null }
        )
    }

    val filteredRequests = remember(uiState.requests, uiState.activeTypeFilters, uiState.activeStatusFilters, uiState.searchQuery) {
        uiState.requests
            .filter { if (uiState.activeTypeFilters.isEmpty()) true else uiState.activeTypeFilters.contains(it.type) }
            .filter { if (uiState.activeStatusFilters.isEmpty()) true else uiState.activeStatusFilters.contains(it.status) }
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
        if (uiState.currentSubScreen != SupportScreenTypeEnum.HUB) {
            viewModel.onEvent(SupportEvent.Navigate(SupportScreenTypeEnum.HUB))
        } else {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(SupportEvent.Refresh)
    }

    Scaffold(
        topBar = {
            var draftSearchQuery by remember { mutableStateOf(uiState.searchQuery) }
            
            LaunchedEffect(uiState.searchQuery) { draftSearchQuery = uiState.searchQuery }

            GenericTitle(
                title = when (uiState.currentSubScreen) {
                    SupportScreenTypeEnum.HUB -> Loc.Support.hub_title
                    SupportScreenTypeEnum.DETAIL -> Loc.Support.request_number.format(uiState.selectedRequest?.number ?: 0)
                    SupportScreenTypeEnum.NEW_FORM -> Loc.Support.new_request
                },
                onBack = {
                    if (uiState.currentSubScreen != SupportScreenTypeEnum.HUB) {
                        viewModel.onEvent(SupportEvent.Navigate(SupportScreenTypeEnum.HUB))
                    } else {
                        onBack()
                    }
                },
                isLoading = uiState.isLoading,
                onRefresh = { viewModel.onEvent(SupportEvent.Refresh) },
                isSearchVisible = uiState.isSearchVisible && uiState.currentSubScreen == SupportScreenTypeEnum.HUB,
                searchQuery = draftSearchQuery,
                onSearchQueryChange = { draftSearchQuery = it },
                onSearchClose = { 
                    viewModel.onEvent(SupportEvent.UpdateSearchQuery(""))
                    viewModel.onEvent(SupportEvent.ToggleSearch) 
                },
                onSearchTriggered = {
                    viewModel.onEvent(SupportEvent.UpdateSearchQuery(draftSearchQuery))
                },
                actions = {
                    if (uiState.currentSubScreen == SupportScreenTypeEnum.HUB) {
                        TitleActionIcon(icon = Icons.Default.Search, onClick = { viewModel.onEvent(SupportEvent.ToggleSearch) })
                        TitleActionIcon(icon = Icons.Default.FilterList, onClick = { showFilterSheet = true })
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.currentSubScreen == SupportScreenTypeEnum.HUB) {
                UnifiedFAB(
                    onClick = { showTypeSheet = true },
                    contentDescription = Loc.General.add
                )
            }
        },
        modifier = Modifier.fillMaxSize().navigationBarsPadding()
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.onEvent(SupportEvent.Refresh) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState.currentSubScreen) {
                    SupportScreenTypeEnum.HUB -> {
                        if (filteredRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = Loc.Support.welcome_title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = Loc.Support.welcome_msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
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
                                                Text(Loc.General.seeAll)
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
                    SupportScreenTypeEnum.DETAIL -> {
                        uiState.selectedRequest?.let { request ->
                            val appUserId = viewModel.getAppUserId()
                            val authUid = viewModel.getAuthUserId()
                            val comments by viewModel.comments.collectAsState(initial = emptyList())
                            
                            SupportDetailCard(
                                request = request.copy(comments = comments),
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
                                isOwner = { it.userId == appUserId || it.userId == authUid },
                                onShowMessage = { msg, type -> infoMessage = msg to type }
                            )
                        }
                    }
                    SupportScreenTypeEnum.NEW_FORM -> {
                        SupportNewRequestForm(
                            text = uiState.requestText,
                            onTextChange = { viewModel.onEvent(SupportEvent.UpdateRequestText(it)) },
                            isSending = uiState.isSending,
                            selectedType = uiState.newRequestType,
                            onSubmit = { viewModel.onEvent(SupportEvent.SubmitRequest(uiState.newRequestType, context)) }
                        )
                    }
                }
            }
        }
    }
}
