package com.jtool.android.features.calendar.components.type_picker_sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.calendar.enums.CalendarEventTypeEnum
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun TypePickerSheet(
    selectedType: CalendarEventTypeEnum,
    onTypeSelected: (CalendarEventTypeEnum) -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Calendar.eventType,
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
                    .heightIn(max = 300.dp)
            ) {
                items(CalendarEventTypeEnum.entries) { type ->
                    val isSelected = selectedType == type
                    
                    ListItem(
                        headlineContent = { 
                            Text(if (type == CalendarEventTypeEnum.SHIFT) Loc.Calendar.shift else Loc.Calendar.common)
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (type == CalendarEventTypeEnum.SHIFT) Icons.Default.Work else Icons.Default.Category,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                RadioButton(selected = true, onClick = null)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp).clickable {
                            onTypeSelected(type)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
