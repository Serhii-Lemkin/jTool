package com.jtool.android.features.calendar.settings.components.grid_style_picker_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.core.models.calendar.enums.GridEventStyleEnum
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun GridStylePickerSheet(
    selectedStyle: GridEventStyleEnum,
    onSelectStyle: (GridEventStyleEnum) -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = CalendarLoc.gridEventStyle,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = CalendarLoc.gridEventStyleSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ActionListItem(
                headline = CalendarLoc.gridEventStyleDots,
                leadingContent = { Icon(Icons.Default.GridOn, null) },
                trailingContent = {
                    if (selectedStyle == GridEventStyleEnum.DOTS) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = {
                    onSelectStyle(GridEventStyleEnum.DOTS)
                    onDismiss()
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = CalendarLoc.gridEventStyleChips,
                leadingContent = { Icon(Icons.Default.ViewDay, null) },
                trailingContent = {
                    if (selectedStyle == GridEventStyleEnum.CHIPS) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = {
                    onSelectStyle(GridEventStyleEnum.CHIPS)
                    onDismiss()
                }
            )
        }
    }
}
