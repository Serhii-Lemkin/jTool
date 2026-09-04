package com.jtool.android.features.shopping.components.filter_selection_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum
import com.jtool.android.features.shopping.AisleIcons
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun FilterSelectionSheet(
    activeAisles: List<ShoppingAisleEnum>,
    selectedAisleFilters: Set<ShoppingAisleEnum>,
    currentFilterType: String,
    onToggleAisle: (ShoppingAisleEnum) -> Unit,
    onShowAll: () -> Unit,
    onShowInactive: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.ShopList.filterItems
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            item {
                FilterOptionItem(
                    label = Loc.ShopList.all,
                    icon = Icons.Default.FilterList,
                    isSelected = currentFilterType == "All" && selectedAisleFilters.isEmpty(),
                    onClick = { onShowAll(); onDismiss() }
                )
            }
            
            items(activeAisles) { aisle ->
                FilterOptionItem(
                    label = Loc.getAisleName(aisle),
                    icon = AisleIcons[aisle]!!,
                    isSelected = selectedAisleFilters.contains(aisle),
                    onClick = { onToggleAisle(aisle) }
                )
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            item {
                FilterOptionItem(
                    label = Loc.ShopList.unactive,
                    icon = Icons.Default.CheckBox,
                    isSelected = currentFilterType == "Unactive",
                    onClick = { onShowInactive(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun FilterOptionItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
