package com.jtool.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.core.models.user.enums.LauncherGestureEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.settings.groups.components.group_selection_sheet.GroupSelectionSheet
import com.jtool.android.ui.components.launcher.*
import com.jtool.android.ui.navigation.AppProperties
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun LauncherScreen(
    onWorkTimeClick: () -> Unit,
    onTimeCalcClick: () -> Unit,
    onShopListClick: () -> Unit,
    onNotesClick: () -> Unit,
    onLinksClick: () -> Unit,
    onCleaningClick: () -> Unit,
    onTrendifyClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onVaultsClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onSupportClick: () -> Unit,
    settingsManager: SettingsManager,
    dataManager: DataManager? = null,
    currentGroupId: String?,
    groups: Map<String, GroupInfo>,
    onJoinGroupClick: () -> Unit,
    onGroupSelected: (String) -> Unit,
    isAdmin: Boolean,
    onAdminClick: () -> Unit,
    onAccountClick: () -> Unit,
    currentUser: FirebaseUser?,
    notificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var showGroupSheet by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var showHiddenSection by remember { mutableStateOf(false) }
    var showGesturesSection by remember { mutableStateOf(false) }

    var gridColumnCount by remember { mutableIntStateOf(settingsManager.getGridColumnCount()) }
    var appOrderState by remember { mutableStateOf(settingsManager.getAppOrder()) }
    var hiddenAppsState by remember { mutableStateOf(settingsManager.getHiddenApps()) }
    var gesturesState by remember { mutableStateOf(settingsManager.getLauncherGestures()) }

    var originalColumnCount by remember { mutableIntStateOf(gridColumnCount) }
    var originalAppOrder by remember { mutableStateOf(appOrderState) }
    var originalHiddenApps by remember { mutableStateOf(hiddenAppsState) }
    var originalGestures by remember { mutableStateOf(gesturesState) }

    var userName by remember(settingsManager, currentUser) {
        mutableStateOf(
            settingsManager.getUserName().ifEmpty { currentUser?.displayName ?: "" }
        )
    }

    DisposableEffect(settingsManager, currentUser) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "user_name" -> {
                    userName = settingsManager.getUserName().ifEmpty { currentUser?.displayName ?: "" }
                }
                "launcher_config", "grid_column_count", "app_order", "hidden_apps" -> {
                    if (!isEditMode) {
                        gridColumnCount = settingsManager.getGridColumnCount()
                        appOrderState = settingsManager.getAppOrder()
                        hiddenAppsState = settingsManager.getHiddenApps()
                        gesturesState = settingsManager.getLauncherGestures()
                    }
                }
            }
        }
        settingsManager.registerListener(listener)
        onDispose {
            settingsManager.unregisterListener(listener)
        }
    }

    val orderedApps = remember(appOrderState) { getOrderedApps(appOrderState) }

    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var focusOnSearch by remember { mutableStateOf(false) }

    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()

    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    val clickHandlers = remember {
        mapOf(
            AppScreenEnum.WORK_TIME to onWorkTimeClick,
            AppScreenEnum.TIME_CALC to onTimeCalcClick,
            AppScreenEnum.SHOP_LIST to onShopListClick,
            AppScreenEnum.NOTES to onNotesClick,
            AppScreenEnum.LINKS to onLinksClick,
            AppScreenEnum.CLEANING to onCleaningClick,
            AppScreenEnum.TRENDIFY to onTrendifyClick,
            AppScreenEnum.CALENDAR to onCalendarClick,
            AppScreenEnum.VAULTS to onVaultsClick
        )
    }

    fun triggerGestureAction(actionId: String) {
        if (actionId.isEmpty()) return
        when (actionId) {
            AppScreenEnum.SETTINGS.id -> onSettingsClick()
            AppScreenEnum.SUPPORT_HUB.id -> onSupportClick()
            AppScreenEnum.ADMIN_PANEL.id -> onAdminClick()
            else -> {
                val app = AppProperties.allApps.find { it.id.id == actionId }
                if (app != null) {
                    clickHandlers[app.id]?.invoke()
                }
            }
        }
    }

    fun enterEditMode() {
        if (!isSearchVisible) {
            originalColumnCount = gridColumnCount
            originalAppOrder = appOrderState
            originalHiddenApps = hiddenAppsState
            originalGestures = gesturesState
            isEditMode = true
        }
    }

    fun saveEditChanges() {
        val config = com.jtool.android.core.models.user.LauncherConfig(
            appOrder = appOrderState,
            hiddenApps = hiddenAppsState,
            gridColumnCount = gridColumnCount,
            gestures = gesturesState
        )
        settingsManager.saveLauncherConfig(config)
        scope.launch { dataManager?.pushUserSettings() }
        isEditMode = false
    }

    fun discardEditChanges() {
        gridColumnCount = originalColumnCount
        appOrderState = originalAppOrder
        hiddenAppsState = originalHiddenApps
        gesturesState = originalGestures
        isEditMode = false
    }

    fun assignGesture(gestureId: String, actionId: String) {
        val updated = gesturesState.toMutableMap()
        if (actionId.isEmpty()) {
            updated.remove(gestureId)
        } else {
            updated[gestureId] = actionId
        }
        gesturesState = updated
    }

    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            if (focusOnSearch) searchFocusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
            focusOnSearch = false
        }
    }

    val nestedScrollConnection = remember(isSearchVisible, isEditMode, searchQuery) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy > 5f && isSearchVisible && searchQuery.isEmpty()) {
                    isSearchVisible = false
                    focusOnSearch = false
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < -5f && !isSearchVisible && !isEditMode) {
                    focusOnSearch = false
                    isSearchVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val visibleApps = remember(orderedApps, hiddenAppsState, searchQuery) {
        val apps = orderedApps.filter { !hiddenAppsState.contains(it.id.id) }
        if (searchQuery.isBlank()) apps
        else apps.filter { it.getName().contains(searchQuery, ignoreCase = true) }
    }

    val hiddenAppsList = remember(orderedApps, hiddenAppsState) {
        orderedApps.filter { hiddenAppsState.contains(it.id.id) }
    }

    val groupName = remember(currentGroupId, groups) {
        currentGroupId?.let { id ->
            val group = groups[id]
            when {
                group?.isPrivate == true -> Loc.Settings.privateGroup
                !group?.name.isNullOrBlank() -> group?.name
                else -> Loc.General.refreshing
            }
        }
    }

    if (isSearchVisible) {
        BackHandler {
            isSearchVisible = false
            searchQuery = ""
        }
    } else if (isEditMode) {
        BackHandler { discardEditChanges() }
    }

    if (showGroupSheet) {
        GroupSelectionSheet(
            groups = groups,
            currentGroupId = currentGroupId,
            onGroupSelected = onGroupSelected,
            onDismiss = { showGroupSheet = false }
        )
    }

    fun toggleAppVisibility(appId: String) {
        val updated = hiddenAppsState.toMutableList()
        if (updated.contains(appId)) {
            updated.remove(appId)
        } else {
            updated.add(appId)
        }
        hiddenAppsState = updated
    }

    fun swapApps(fromIndex: Int, toIndex: Int) {
        if (fromIndex in visibleApps.indices && toIndex in visibleApps.indices && fromIndex != toIndex) {
            val list = orderedApps.map { it.id.id }.toMutableList()
            val idFrom = visibleApps[fromIndex].id.id
            val idTo = visibleApps[toIndex].id.id
            val idx1 = list.indexOf(idFrom)
            val idx2 = list.indexOf(idTo)
            if (idx1 != -1 && idx2 != -1) {
                val item = list.removeAt(idx1)
                list.add(idx2, item)
                appOrderState = list
            }
        }
    }

    val gestureModifier = if (!isEditMode && !isSearchVisible) {
        Modifier.pointerInput(gesturesState) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (abs(horizontalDragOffset) > 80f) {
                        val gesture = if (horizontalDragOffset > 0) LauncherGestureEnum.SWIPE_RIGHT else LauncherGestureEnum.SWIPE_LEFT
                        val actionId = gesturesState[gesture.id]
                        if (!actionId.isNullOrEmpty()) {
                            triggerGestureAction(actionId)
                        }
                    }
                    horizontalDragOffset = 0f
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    horizontalDragOffset += dragAmount
                }
            )
        }
    } else Modifier

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .then(gestureModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isEditMode) {
            LauncherEditHeader(
                gridColumnCount = gridColumnCount,
                onColumnSelected = { cols ->
                    gridColumnCount = cols
                },
                onSave = { saveEditChanges() },
                onHelpClick = {
                    com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "app-features")
                }
            )
        } else {
            LauncherTopBar(
                onSupportClick = onSupportClick,
                onSettingsClick = onSettingsClick,
                isAdmin = isAdmin,
                currentUser = currentUser,
                onAdminClick = onAdminClick,
                notificationCount = notificationCount,
                onNotificationsClick = onNotificationsClick
            )

            AnimatedVisibility(
                visible = !isSearchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    LauncherGreetingHeader(
                        currentUser = currentUser,
                        userName = userName,
                        groupName = groupName,
                        groups = groups,
                        onGroupClick = { if (groups.isNotEmpty()) showGroupSheet = true else onJoinGroupClick() },
                        onAccountClick = onAccountClick,
                        onSearchClick = {
                            focusOnSearch = true
                            isSearchVisible = true
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester),
                        placeholder = { Text(Loc.Launcher.searchApps) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            } else {
                                IconButton(onClick = {
                                    isSearchVisible = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).nestedScroll(nestedScrollConnection)) {
            LauncherAppGrid(
                visibleApps = visibleApps,
                hiddenAppsState = hiddenAppsState,
                gridColumnCount = gridColumnCount,
                isEditMode = isEditMode && !isSearchVisible,
                clickHandlers = clickHandlers,
                onToggleVisibility = { toggleAppVisibility(it) },
                onEnableEditMode = { enterEditMode() },
                onSwapApps = { from, to -> swapApps(from, to) },
                state = gridState
            )
        }

        if (isEditMode) {
            LauncherHiddenAppsSection(
                hiddenAppsList = hiddenAppsList,
                gridColumnCount = gridColumnCount,
                showHiddenSection = showHiddenSection,
                onToggleShowSection = { showHiddenSection = !showHiddenSection },
                onUnhideApp = { toggleAppVisibility(it) }
            )

            LauncherGesturesSection(
                gesturesMap = gesturesState,
                showGesturesSection = showGesturesSection,
                onToggleShowSection = { showGesturesSection = !showGesturesSection },
                onAssignGesture = { gestureId, actionId -> assignGesture(gestureId, actionId) }
            )
        }
    }
}
