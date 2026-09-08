package com.jtool.android.features.tracker.components.tracker_filter_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.tracker.enums.TrackerTimeFilterEnum
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun TrackerFilterSheet(
    currentFilter: TrackerTimeFilterEnum,
    onFilterSelected: (TrackerTimeFilterEnum) -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Tracker.filter,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TrackerTimeFilterEnum.entries.forEach { filter ->
                val label = when (filter) {
                    TrackerTimeFilterEnum.ALL -> Loc.Tracker.filterAll
                    TrackerTimeFilterEnum.LAST_DAY -> Loc.Tracker.filterLastDay
                    TrackerTimeFilterEnum.LAST_MONTH -> Loc.Tracker.filterLastMonth
                    TrackerTimeFilterEnum.LAST_3_MONTHS -> Loc.Tracker.filterLast3Months
                    TrackerTimeFilterEnum.LAST_6_MONTHS -> Loc.Tracker.filterLast6Months
                    TrackerTimeFilterEnum.LAST_YEAR -> Loc.Tracker.filterLastYear
                }
                
                val isSelected = currentFilter == filter
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onFilterSelected(filter)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
