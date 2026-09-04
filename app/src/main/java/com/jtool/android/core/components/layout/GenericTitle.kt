package com.jtool.android.core.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import androidx.compose.material.icons.filled.Refresh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericTitle(
    title: String,
    onBack: (() -> Unit)? = null,
    isLoading: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    isSearchVisible: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchClose: () -> Unit = {},
    onSearchTriggered: () -> Unit = {},
    searchPlaceholder: String = Loc.General.search,
    actions: @Composable RowScope.() -> Unit = {},
    customTitle: @Composable (() -> Unit)? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
) {
    Column {
        if (isSearchVisible) {
            CenterAlignedTopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        placeholder = { Text(searchPlaceholder) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onSearchTriggered) {
                                    Icon(Icons.Default.Search, null)
                                }
                                IconButton(onClick = onSearchClose) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSearchTriggered() }
                        )
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(navigationIcon, Loc.General.back)
                        }
                    }
                }
            )
        } else {
            TopAppBar(
                title = {
                    if (customTitle != null) {
                        customTitle()
                    } else {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = navigationIcon,
                                contentDescription = Loc.General.back
                            )
                        }
                    }
                },
                actions = {
                    actions()
                }
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun TitleActionIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = LocalContentColor.current
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
fun TitleDropdownMenu(
    icon: ImageVector = Icons.Default.MoreVert,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            content = {
                content()
            }
        )
    }
}
