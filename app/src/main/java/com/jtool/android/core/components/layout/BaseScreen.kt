package com.jtool.android.core.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    title: String,
    isLoading: Boolean = false,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null, // Shared reload function: deletes/clears local data cache and re-fetches from source of truth (Firestore)
    isSearchVisible: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchClose: () -> Unit = {},
    onSearchTriggered: () -> Unit = {},
    searchPlaceholder: String = "",
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            GenericTitle(
                title = title,
                onBack = onBack,
                isLoading = false,
                onRefresh = onRefresh,
                isSearchVisible = isSearchVisible,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchClose = onSearchClose,
                onSearchTriggered = onSearchTriggered,
                searchPlaceholder = searchPlaceholder,
                actions = actions
            )
        },
        floatingActionButton = floatingActionButton,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(paddingValues)
            AppLoader(isLoading = isLoading)
        }
    }
}
