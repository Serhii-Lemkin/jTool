package com.jtool.android.features.support.components.support_filter_slip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import com.jtool.android.features.support.screens.SupportTypeEnumIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportFilterSlip(
    activeTypes: Set<SupportTypeEnum>,
    activeStatuses: Set<SupportStatusEnum>,
    onToggleType: (SupportTypeEnum) -> Unit,
    onToggleStatus: (SupportStatusEnum) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                // Column 1: Ticket Type
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SupportTypeEnum.entries.forEach { type ->
                        FilterItem(
                            label = when(type) {
                                SupportTypeEnum.BUG -> Loc.support_type_bug
                                SupportTypeEnum.FEATURE -> Loc.support_type_feature
                                SupportTypeEnum.CHANGE -> Loc.support_type_change
                                SupportTypeEnum.CRASH -> Loc.support_type_crash
                            },
                            isSelected = activeTypes.contains(type),
                            onClick = { onToggleType(type) }
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                // Column 2: Progress
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SupportStatusEnum.entries.forEach { status ->
                        FilterItem(
                            label = when(status) {
                                SupportStatusEnum.OPENED -> "Opened"
                                SupportStatusEnum.IN_PROGRESS -> "In Progress"
                                SupportStatusEnum.CLOSED -> "Closed"
                                SupportStatusEnum.CUT -> "Cut"
                            },
                            isSelected = activeStatuses.contains(status),
                            onClick = { onToggleStatus(status) }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(Loc.general_close)
            }
        }
    }
}

@Composable
private fun FilterItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
