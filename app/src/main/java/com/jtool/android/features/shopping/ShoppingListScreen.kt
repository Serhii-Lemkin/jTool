package com.jtool.android.features.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.components.layout.CompactActionListItem
import com.jtool.android.core.components.layout.BaseScreen
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.measurement.MeasurementManager
import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.features.shopping.components.AisleIcons
import com.jtool.android.features.shopping.components.ShoppingItemRow
import com.jtool.android.features.shopping.components.aisle_selection_sheet.AisleSelectionSheet
import com.jtool.android.features.shopping.components.filter_selection_sheet.FilterSelectionSheet
import com.jtool.android.features.shopping.components.item_delete_confirm_sheet.ItemDeleteConfirmSheet
import com.jtool.android.features.shopping.components.item_edit_sheet.ItemEditSheet
import com.jtool.android.features.shopping.components.shopping_item_action_sheet.ShoppingItemActionSheet
import com.jtool.android.ui.components.common.MessageSheet
import com.jtool.android.ui.components.common.MessageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    manager: ShoppingListManager,
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val measurementManager = remember(context) { MeasurementManager(context) }
    
    val viewModel: ShoppingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ShoppingViewModel(manager, settingsManager, dataManager, measurementManager)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<Pair<String, MessageType>?>(null) }
    var amountInputText by remember { mutableStateOf("") }
    var isUnitFocused by remember { mutableStateOf(false) }

    var isCompactAddBox by remember { mutableStateOf(settingsManager.isShoppingAddBoxCompact()) }
    val setCompactAddBox: (Boolean) -> Unit = { compact ->
        if (isCompactAddBox != compact) {
            isCompactAddBox = compact
            settingsManager.setShoppingAddBoxCompact(compact)
        }
    }

    val addBoxGestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                if (dragAmount > 15f) {
                    setCompactAddBox(true)
                } else if (dragAmount < -15f) {
                    setCompactAddBox(false)
                }
            }
        )
    }

    if (uiState.showAislePicker) {
        AisleSelectionSheet(
            selectedAisle = uiState.selectedItem?.aisle ?: uiState.selectedAisle,
            onAisleSelected = { viewModel.onEvent(ShoppingEvent.SelectAisle(it)) },
            onDismiss = { 
                viewModel.onEvent(ShoppingEvent.SelectItem(null))
                viewModel.onEvent(ShoppingEvent.ToggleAislePicker(false)) 
            }
        )
    }

    if (showFilterSheet) {
        FilterSelectionSheet(
            activeAisles = uiState.activeAisles,
            selectedAisleFilters = uiState.activeAisleFilters,
            currentFilterType = uiState.filterType,
            onToggleAisle = { viewModel.onEvent(ShoppingEvent.ToggleAisleFilter(it)) },
            onShowAll = { viewModel.onEvent(ShoppingEvent.ClearAisleFilters) },
            onShowInactive = { viewModel.onEvent(ShoppingEvent.UpdateFilter("Unactive")) },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (uiState.itemToEdit != null) {
        ItemEditSheet(
            item = uiState.itemToEdit!!,
            measurementManager = measurementManager,
            onDismiss = { viewModel.onEvent(ShoppingEvent.RequestEdit(null)) },
            onConfirm = { name, amount, unit, aisle ->
                viewModel.onEvent(ShoppingEvent.ConfirmEdit(uiState.itemToEdit!!, name, amount, unit, aisle))
            }
        )
    }

    if (uiState.itemToDelete != null) {
        ItemDeleteConfirmSheet(
            onDismiss = { viewModel.onEvent(ShoppingEvent.RequestDelete(null)) },
            onConfirm = { viewModel.onEvent(ShoppingEvent.ConfirmDelete) }
        )
    }

    if (infoMessage != null) {
        MessageSheet(
            message = infoMessage!!.first,
            type = infoMessage!!.second,
            onDismiss = { infoMessage = null }
        )
    }

    BaseScreen(
        title = Loc.ShopList.shoppingList,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(ShoppingEvent.Refresh) },
        actions = {
            val filterIcon = when {
                uiState.filterType == "Unactive" -> Icons.Default.CheckBox
                uiState.activeAisleFilters.isNotEmpty() -> {
                    val first = uiState.activeAisleFilters.first()
                    AisleIcons[first] ?: Icons.AutoMirrored.Filled.Label
                }
                else -> Icons.Default.FilterList
            }
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(filterIcon, contentDescription = "Filter")
            }
            com.jtool.android.core.components.layout.TitleDropdownMenu {
                com.jtool.android.core.components.layout.ActionListItem(
                    headline = com.jtool.android.core.localization.domains.GeneralLoc.learnMore,
                    supportingText = Loc.s("shopping_learnMoreSubtitle", "View online guide and feature documentation"),
                    leadingContent = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { com.jtool.android.core.utils.WebUtils.openAppPage(context, settingsManager, dataManager, "shopping-list") }
                )
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                        items(uiState.displayItems, key = { "active_${it.id}" }) { item ->
                            ShoppingItemRow(
                                item = item,
                                measurementManager = measurementManager,
                                onToggle = { checked ->
                                    viewModel.onEvent(ShoppingEvent.ToggleItem(item, checked))
                                },
                                onDelete = {
                                    viewModel.onEvent(ShoppingEvent.RequestDelete(item))
                                },
                                onEdit = {
                                    viewModel.onEvent(ShoppingEvent.RequestEdit(item))
                                },
                                userName = uiState.userNames[item.lastUpdatedBy] ?: item.lastUpdatedBy
                            )
                        }

                        if (uiState.checkedItems.isNotEmpty()) {
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                Text(
                                    text = Loc.ShopList.recentlyChecked,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            items(uiState.checkedItems, key = { "checked_${it.id}" }) { item ->
                                ShoppingItemRow(
                                    item = item,
                                    measurementManager = measurementManager,
                                    onToggle = { checked ->
                                        viewModel.onEvent(ShoppingEvent.ToggleItem(item, checked))
                                    },
                                    onDelete = {
                                        viewModel.onEvent(ShoppingEvent.RequestDelete(item))
                                    },
                                    onEdit = {
                                        viewModel.onEvent(ShoppingEvent.RequestEdit(item))
                                    },
                                    userName = uiState.userNames[item.lastUpdatedBy] ?: item.lastUpdatedBy
                                )
                            }
                        }
                        
                        item {
                            Spacer(Modifier.height(100.dp))
                        }
                    }

                    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val extraImePadding = (imeBottom - navBottom).coerceAtLeast(0.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = extraImePadding)
                    ) {
                        val unitSuggestions = remember(uiState.newItemUnit) {
                            measurementManager.getSuggestions(uiState.newItemUnit)
                        }

                        if (isUnitFocused && unitSuggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            ) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(unitSuggestions) { (_, localizedName) ->
                                        FilterChip(
                                            selected = uiState.newItemUnit.equals(localizedName, ignoreCase = true),
                                            onClick = {
                                                viewModel.onEvent(ShoppingEvent.UpdateNewItemUnit(localizedName))
                                            },
                                            label = {
                                                Text(
                                                    text = localizedName,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.suggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp
                            ) {
                                Column {
                                    uiState.suggestions.forEach { suggestionText ->
                                        TextButton(
                                            onClick = {
                                                viewModel.onEvent(ShoppingEvent.RestoreItem(suggestionText))
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.History, 
                                                    contentDescription = null, 
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    text = suggestionText,
                                                    modifier = Modifier.weight(1f),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = Loc.ShopList.restore,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(addBoxGestureModifier)
                                .padding(bottom = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { setCompactAddBox(!isCompactAddBox) }
                                        .padding(top = 6.dp, bottom = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(32.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.onEvent(ShoppingEvent.ToggleAislePicker(true)) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            AisleIcons[uiState.selectedAisle] ?: Icons.AutoMirrored.Filled.Label,
                                            contentDescription = "Select Aisle",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        TextField(
                                            value = uiState.newItemText,
                                            onValueChange = { viewModel.onEvent(ShoppingEvent.UpdateNewItemText(it)) },
                                            placeholder = { Text(Loc.ShopList.addItem) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            )
                                        )

                                        if (!isCompactAddBox) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextField(
                                                    value = amountInputText,
                                                    onValueChange = { text ->
                                                        amountInputText = text
                                                        val amt = text.toDoubleOrNull()
                                                        viewModel.onEvent(ShoppingEvent.UpdateNewItemAmount(amt))
                                                    },
                                                    placeholder = { Text(Loc.ShopList.amount, fontSize = 12.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent
                                                    )
                                                )

                                                TextField(
                                                    value = uiState.newItemUnit,
                                                    onValueChange = { viewModel.onEvent(ShoppingEvent.UpdateNewItemUnit(it)) },
                                                    placeholder = { Text(Loc.ShopList.unit, fontSize = 12.sp) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .onFocusChanged { isUnitFocused = it.isFocused },
                                                    singleLine = true,
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { setCompactAddBox(!isCompactAddBox) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCompactAddBox) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                                            contentDescription = "Toggle Amount Fields",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }

                                    Spacer(Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            if (uiState.newItemText.isNotBlank()) {
                                                val trimmedText = uiState.newItemText.trim()
                                                val existingItem = uiState.items.find { it.text.equals(trimmedText, ignoreCase = true) }
                                                
                                                if (existingItem != null && !existingItem.isChecked) {
                                                    infoMessage = Loc.ShopList.itemAlreadyInList to MessageType.INFO
                                                } else {
                                                    viewModel.onEvent(ShoppingEvent.AddItem)
                                                    amountInputText = ""
                                                }
                                            }
                                        },
                                        enabled = uiState.newItemText.isNotBlank(),
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                    Spacer(Modifier.width(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
