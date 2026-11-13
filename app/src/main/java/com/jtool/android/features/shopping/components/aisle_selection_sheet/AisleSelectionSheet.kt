package com.jtool.android.features.shopping.components.aisle_selection_sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum
import com.jtool.android.features.shopping.components.AisleIcons
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun AisleSelectionSheet(
    selectedAisle: ShoppingAisleEnum?,
    onAisleSelected: (ShoppingAisleEnum) -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.ShopList.selectAisle
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AisleIcons.toList()) { (aisle, icon) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAisleSelected(aisle)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAisle == aisle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = if (selectedAisle == aisle) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        Spacer(Modifier.width(12.dp))
                        Text(text = Loc.getAisleName(aisle), fontSize = 14.sp, fontWeight = if (selectedAisle == aisle) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
