package com.jtool.android.features.shopping.components.item_edit_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.measurement.MeasurementManager
import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum
import com.jtool.android.features.shopping.AisleIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditSheet(
    item: ShoppingItem,
    measurementManager: MeasurementManager,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newAmount: Double?, newUnit: String, newAisle: ShoppingAisleEnum) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    var nameText by remember { mutableStateOf(item.text) }
    var amountText by remember {
        mutableStateOf(
            item.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var unitText by remember { mutableStateOf(measurementManager.getLocalizedUnitName(item.unit)) }
    var selectedAisle by remember { mutableStateOf(item.aisle) }

    val unitSuggestions = remember(unitText) {
        measurementManager.getSuggestions(unitText)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = Loc.ShopList.editItem,
                style = MaterialTheme.typography.titleLarge
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(Loc.ShopList.itemNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(Loc.ShopList.amount) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text(Loc.ShopList.unit) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (unitSuggestions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(unitSuggestions) { (_, localizedName) ->
                            FilterChip(
                                selected = unitText.equals(localizedName, ignoreCase = true),
                                onClick = { unitText = localizedName },
                                label = { Text(localizedName) }
                            )
                        }
                    }
                }

                Text(
                    text = Loc.ShopList.selectAisle,
                    style = MaterialTheme.typography.titleMedium
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShoppingAisleEnum.entries.chunked(2).forEach { rowAisles ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowAisles.forEach { aisle ->
                                val isSelected = selectedAisle == aisle
                                val icon = AisleIcons[aisle] ?: Icons.AutoMirrored.Filled.Label
                                
                                Surface(
                                    onClick = { selectedAisle = aisle },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = Loc.getAisleName(aisle),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowAisles.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        val parsedAmount = amountText.toDoubleOrNull()
                        onConfirm(nameText.trim(), parsedAmount, unitText.trim(), selectedAisle)
                    }
                },
                enabled = nameText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Loc.General.save)
            }
        }
    }
}
