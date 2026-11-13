package com.jtool.android.features.shopping.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.KebabDining
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.CompactActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.measurement.MeasurementManager
import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum
import com.jtool.android.features.shopping.components.shopping_item_action_sheet.ShoppingItemActionSheet

val AisleIcons = mapOf(
    ShoppingAisleEnum.COMMON to Icons.AutoMirrored.Filled.Label,
    ShoppingAisleEnum.VEGGIES to Icons.Default.Eco,
    ShoppingAisleEnum.MEAT to Icons.Default.KebabDining,
    ShoppingAisleEnum.FISH to Icons.Default.SetMeal,
    ShoppingAisleEnum.DAIRY to Icons.Default.Egg,
    ShoppingAisleEnum.BAKERY to Icons.Default.BakeryDining,
    ShoppingAisleEnum.DRINKS to Icons.Default.LocalDrink,
    ShoppingAisleEnum.FROZEN to Icons.Default.AcUnit,
    ShoppingAisleEnum.SNACKS to Icons.Default.Cookie,
    ShoppingAisleEnum.HOUSEHOLD to Icons.Default.CleaningServices,
    ShoppingAisleEnum.PERSONAL to Icons.Default.Spa,
    ShoppingAisleEnum.PETS to Icons.Default.Pets,
    ShoppingAisleEnum.PANTRY to Icons.Default.BreakfastDining
)

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    measurementManager: MeasurementManager,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    userName: String
) {
    var showActionSheet by remember { mutableStateOf(false) }

    val formattedAmount = remember(item.amount, item.unit) {
        if (item.amount != null && item.amount > 0) {
            val amtStr = if (item.amount % 1.0 == 0.0) item.amount.toLong().toString() else item.amount.toString()
            val unitStr = measurementManager.getLocalizedUnitName(item.unit)
            if (unitStr.isNotBlank()) "$amtStr $unitStr" else amtStr
        } else if (item.unit.isNotBlank()) {
            measurementManager.getLocalizedUnitName(item.unit)
        } else {
            ""
        }
    }

    if (showActionSheet) {
        ShoppingItemActionSheet(
            onEdit = onEdit,
            onDelete = onDelete,
            onDismiss = { showActionSheet = false }
        )
    }

    CompactActionListItem(
        onClick = null,
        headline = item.text,
        isStrikethrough = item.isChecked,
        metadataText = "${Loc.General.by} $userName",
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { onToggle(it) },
                        modifier = Modifier.scale(0.85f).offset(x = (-4).dp)
                    )
                }
                Icon(
                    imageVector = AisleIcons[item.aisle] ?: Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (formattedAmount.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = formattedAmount,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    IconButton(
                        onClick = { showActionSheet = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    )
}
