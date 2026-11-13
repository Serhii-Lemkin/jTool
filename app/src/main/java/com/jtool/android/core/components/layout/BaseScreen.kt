package com.jtool.android.core.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    title: String,
    isLoading: Boolean = false,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    isSearchVisible: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchClose: () -> Unit = {},
    onSearchTriggered: () -> Unit = {},
    searchPlaceholder: String = "",
    actions: @Composable RowScope.() -> Unit = {},
    hasNotifications: Boolean = false,
    notificationCount: Int = 0,
    onNotificationsClick: (() -> Unit)? = null,
    floatingActionButton: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    isTitleEditable: Boolean = false,
    onTitleChange: (String) -> Unit = {},
    titlePlaceholder: String = "",
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            GenericTitle(
                title = title,
                onBack = onBack,
                onRefresh = onRefresh,
                isSearchVisible = isSearchVisible,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchClose = onSearchClose,
                onSearchTriggered = onSearchTriggered,
                searchPlaceholder = searchPlaceholder,
                actions = actions,
                isTitleEditable = isTitleEditable,
                onTitleChange = onTitleChange,
                titlePlaceholder = titlePlaceholder
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { onRefresh?.invoke() },
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                content(PaddingValues(0.dp))
            }

            RedContainer(
                screenTitle = title,
                hasNotifications = hasNotifications,
                notificationCount = notificationCount,
                onNotificationsClick = onNotificationsClick,
                content = floatingActionButton
            )
        }
    }
}
