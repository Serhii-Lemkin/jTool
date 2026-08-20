package com.jtool.android.core.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun UnifiedFAB(
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String? = null,
    onClick: () -> Unit,
    dropdownContent: (@Composable ColumnScope.(onDismiss: () -> Unit) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val layoutDirection = LocalLayoutDirection.current
    val alignment = if (layoutDirection == LayoutDirection.Rtl) Alignment.BottomStart else Alignment.BottomEnd

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = alignment
    ) {
        Box {
            FloatingActionButton(
                onClick = {
                    if (dropdownContent != null) {
                        expanded = true
                    } else {
                        onClick()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = icon, contentDescription = contentDescription)
            }
            
            if (dropdownContent != null) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    dropdownContent { expanded = false }
                }
            }
        }
    }
}
