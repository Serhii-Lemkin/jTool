package com.jtool.android.features.calendar.grid.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarGridActionSheet(
    showDeclinedEvents: Boolean = false,
    onToggleShowDeclined: () -> Unit = {},
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = Loc.General.details,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            ActionListItem(
                headline = if (showDeclinedEvents) "Hide declined events" else "See declined events",
                leadingContent = { Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    onToggleShowDeclined()
                    onDismiss()
                }
            )

            Spacer(Modifier.height(8.dp))

            ActionListItem(
                headline = Loc.Calendar.goToSettings,
                leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    onSettings()
                    onDismiss()
                }
            )
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
