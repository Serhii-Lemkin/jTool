package com.jtool.android.features.calendar.components.group_picker_sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.GroupInfo
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun GroupPickerSheet(
    groups: Map<String, GroupInfo>,
    selectedIds: Set<String>,
    onToggleId: (String) -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Calendar.selectGroups,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(groups.values.filter { !it.isPrivate }.toList(), key = { it.id }) { group ->
                    val isSelected = selectedIds.contains(group.id)
                    
                    ListItem(
                        headlineContent = { Text(group.name) },
                        trailingContent = {
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { onToggleId(group.id) }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.padding(horizontal = 16.dp).clickable { onToggleId(group.id) }
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
            ) {
                Text(Loc.General.save)
            }
        }
    }
}
