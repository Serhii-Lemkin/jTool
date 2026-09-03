package com.jtool.android.features.shopping

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.jtool.android.core.models.feature.shopping.ShoppingItem
import com.jtool.android.core.models.feature.shopping.enums.ShoppingAisleEnum
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.DataManager
import com.jtool.android.core.components.layout.CompactActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.features.shopping.components.aisle_selection_slip.AisleSelectionSlip
import com.jtool.android.features.shopping.components.filter_selection_slip.FilterSelectionSlip
import com.jtool.android.features.shopping.components.item_delete_confirm_slip.ItemDeleteConfirmSlip
import com.jtool.android.features.shopping.components.item_rename_slip.ItemRenameSlip

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
    ShoppingAisleEnum.PETS to Icons.Default.Pets
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    manager: ShoppingListManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    dataManager: DataManager,
    onBack: () -> Unit,
    viewModel: ShoppingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ShoppingViewModel(manager, settingsManager, dataManager)
            }
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSlip by remember { mutableStateOf(false) }

    if (uiState.showAislePicker) {
        AisleSelectionSlip(
            selectedAisle = uiState.selectedItem?.aisle ?: uiState.selectedAisle,
            onAisleSelected = { viewModel.onEvent(ShoppingEvent.SelectAisle(it)) },
            onDismiss = { 
                viewModel.onEvent(ShoppingEvent.SelectItem(null))
                viewModel.onEvent(ShoppingEvent.ToggleAislePicker(false)) 
            }
        )
    }

    if (showFilterSlip) {
        FilterSelectionSlip(
            activeAisles = uiState.activeAisles,
            selectedAisleFilters = uiState.activeAisleFilters,
            currentFilterType = uiState.filterType,
            onToggleAisle = { viewModel.onEvent(ShoppingEvent.ToggleAisleFilter(it)) },
            onShowAll = { viewModel.onEvent(ShoppingEvent.ClearAisleFilters) },
            onShowInactive = { viewModel.onEvent(ShoppingEvent.UpdateFilter("Unactive")) },
            onDismiss = { showFilterSlip = false }
        )
    }

    if (uiState.itemToRename != null) {
        ItemRenameSlip(
            currentName = uiState.itemToRename!!.text,
            onDismiss = { viewModel.onEvent(ShoppingEvent.RequestRename(null)) },
            onConfirm = { 
                viewModel.onEvent(ShoppingEvent.ConfirmRename(it))
            }
        )
    }

    if (uiState.itemToDelete != null) {
        ItemDeleteConfirmSlip(
            onDismiss = { viewModel.onEvent(ShoppingEvent.RequestDelete(null)) },
            onConfirm = { viewModel.onEvent(ShoppingEvent.ConfirmDelete) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val filterIcon = when {
                    uiState.filterType == "Unactive" -> Icons.Default.CheckBox
                    uiState.activeAisleFilters.isNotEmpty() -> {
                        val first = uiState.activeAisleFilters.first()
                        AisleIcons[first] ?: Icons.AutoMirrored.Filled.Label
                    }
                    else -> Icons.Default.FilterList
                }
                GenericTitle(
                    title = Loc.shoplist_shoppingList,
                    onBack = onBack,
                    isLoading = uiState.isLoading,
                    onRefresh = { viewModel.onEvent(ShoppingEvent.Refresh) },
                    actions = {
                        IconButton(onClick = { showFilterSlip = true }) {
                            Icon(filterIcon, contentDescription = "Filter")
                        }
                    }
                )
            }
        ) { padding ->
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.onEvent(ShoppingEvent.Refresh) },
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(uiState.displayItems, key = { it.id }) { item ->
                            ShoppingItemRow(
                                item = item,
                                onToggle = { checked ->
                                    viewModel.onEvent(ShoppingEvent.ToggleItem(item, checked))
                                },
                                onDelete = {
                                    viewModel.onEvent(ShoppingEvent.RequestDelete(item))
                                },
                                onRename = {
                                    viewModel.onEvent(ShoppingEvent.RequestRename(item))
                                },
                                onChangeAisle = {
                                    viewModel.onEvent(ShoppingEvent.SelectItem(item))
                                    viewModel.onEvent(ShoppingEvent.ToggleAislePicker(true))
                                },
                                userName = uiState.userNames[item.lastUpdatedBy] ?: item.lastUpdatedBy
                            )
                        }

                        if (uiState.checkedItems.isNotEmpty()) {
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                Text(
                                    text = Loc.shoplist_recentlyChecked,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            items(uiState.checkedItems, key = { it.id }) { item ->
                                ShoppingItemRow(
                                    item = item,
                                    onToggle = { checked ->
                                        viewModel.onEvent(ShoppingEvent.ToggleItem(item, checked))
                                    },
                                    onDelete = {
                                        viewModel.onEvent(ShoppingEvent.RequestDelete(item))
                                    },
                                    onRename = {
                                        viewModel.onEvent(ShoppingEvent.RequestRename(item))
                                    },
                                    onChangeAisle = {
                                        viewModel.onEvent(ShoppingEvent.SelectItem(item))
                                        viewModel.onEvent(ShoppingEvent.ToggleAislePicker(true))
                                    },
                                    userName = uiState.userNames[item.lastUpdatedBy] ?: item.lastUpdatedBy
                                )
                            }
                        }
                        
                        item {
                            Spacer(Modifier.height(100.dp))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .navigationBarsPadding() // Account for system buttons
                            .imePadding() // Avoid keyboard obstruction
                    ) {
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
                                                    text = Loc.shoplist_restore,
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
                                .padding(bottom = 4.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.onEvent(ShoppingEvent.ToggleAislePicker(true)) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        AisleIcons[uiState.selectedAisle] ?: Icons.AutoMirrored.Filled.Label,
                                        contentDescription = "Select Aisle",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                TextField(
                                    value = uiState.newItemText,
                                    onValueChange = { viewModel.onEvent(ShoppingEvent.UpdateNewItemText(it)) },
                                    placeholder = { Text(Loc.shoplist_addItem) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                                
                                IconButton(
                                    onClick = {
                                        if (uiState.newItemText.isNotBlank()) {
                                            val trimmedText = uiState.newItemText.trim()
                                            val existingItem = uiState.items.find { it.text.equals(trimmedText, ignoreCase = true) }
                                            
                                            if (existingItem != null && !existingItem.isChecked) {
                                                Toast.makeText(context, Loc.shoplist_itemAlreadyInList, Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.onEvent(ShoppingEvent.AddItem)
                                                if (existingItem != null && existingItem.isChecked) {
                                                    Toast.makeText(context, Loc.shoplist_restoredExisting, Toast.LENGTH_SHORT).show()
                                                }
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
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onChangeAisle: () -> Unit,
    userName: String
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CompactActionListItem(
        onClick = null,
        headline = item.text,
        isStrikethrough = item.isChecked,
        metadataText = "${Loc.general_by} $userName",
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { onToggle(it) },
                        modifier = Modifier.size(32.dp)
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
            Box {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        text = { Text(Loc.shoplist_changeAisle) },
                        onClick = {
                            onChangeAisle()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                        text = { Text("Rename") },
                        onClick = {
                            onRename()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        text = { Text(Loc.general_delete, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    )
}
