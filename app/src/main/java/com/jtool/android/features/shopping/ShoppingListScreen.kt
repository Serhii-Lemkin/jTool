package com.jtool.android.features.shopping

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.jtool.android.core.models.ShoppingItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import com.jtool.android.core.components.layout.CompactActionListItem
import com.jtool.android.core.components.layout.GenericTitle
import com.jtool.android.core.components.layout.TitleDropdownMenu
import com.jtool.android.core.components.layout.TitleActionIcon

val AisleIcons = mapOf(
    "Common" to Icons.AutoMirrored.Filled.Label,
    "Veggies" to Icons.Default.Eco,
    "Meat" to Icons.Default.KebabDining,
    "Fish" to Icons.Default.SetMeal,
    "Dairy" to Icons.Default.Egg,
    "Bakery" to Icons.Default.BakeryDining,
    "Drinks" to Icons.Default.LocalDrink,
    "Frozen" to Icons.Default.AcUnit,
    "Snacks" to Icons.Default.Cookie,
    "Household" to Icons.Default.CleaningServices,
    "Personal" to Icons.Default.Spa,
    "Pets" to Icons.Default.Pets
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    manager: ShoppingListManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
    onBack: () -> Unit,
    viewModel: ShoppingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ShoppingViewModel(manager, settingsManager, groupSyncManager)
            }
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showAislePicker) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.onEvent(ShoppingEvent.SelectItem(null))
                viewModel.onEvent(ShoppingEvent.ToggleAislePicker(false)) 
            },
            title = { Text(Loc.shoplist_selectAisle) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AisleIcons.toList()) { (key, icon) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onEvent(ShoppingEvent.SelectAisle(key))
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.selectedAisle == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = Loc.getAisleName(key), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (uiState.itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ShoppingEvent.RequestDelete(null)) },
            title = { Text(Loc.general_confirmDeleteTitle) },
            text = { Text(Loc.general_confirmDeleteMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(ShoppingEvent.ConfirmDelete)
                }) {
                    Text(Loc.general_delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ShoppingEvent.RequestDelete(null)) }) {
                    Text(Loc.general_cancel)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val filterIcon = when {
                    uiState.filterType == "Unactive" -> Icons.Default.CheckBox
                    uiState.filterType.startsWith("Aisle:") -> AisleIcons[uiState.filterType.removePrefix("Aisle:")] ?: Icons.AutoMirrored.Filled.Label
                    else -> Icons.Default.FilterList
                }
                GenericTitle(
                    title = Loc.shoplist_shoppingList,
                    onBack = onBack,
                    isLoading = uiState.isLoading,
                    onRefresh = { viewModel.onEvent(ShoppingEvent.Refresh) },
                    actions = {
                        TitleDropdownMenu(icon = filterIcon) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                                text = { Text(Loc.shoplist_all) },
                                onClick = { viewModel.onEvent(ShoppingEvent.UpdateFilter("All")) }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null) },
                                text = { Text(Loc.shoplist_unactive) },
                                onClick = { viewModel.onEvent(ShoppingEvent.UpdateFilter("Unactive")) }
                            )
                            uiState.activeAisles.forEach { aisle ->
                                DropdownMenuItem(
                                    leadingIcon = { Icon(AisleIcons[aisle] ?: Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                                    text = { Text(Loc.getAisleName(aisle)) },
                                    onClick = { viewModel.onEvent(ShoppingEvent.UpdateFilter("Aisle:$aisle")) }
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.onEvent(ShoppingEvent.Refresh) },
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
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

                    Spacer(modifier = Modifier.height(8.dp))

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

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.onEvent(ShoppingEvent.ToggleAislePicker(true)) }) {
                            Icon(
                                AisleIcons[uiState.selectedAisle] ?: Icons.AutoMirrored.Filled.Label,
                                contentDescription = "Select Aisle",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedTextField(
                            value = uiState.newItemText,
                            onValueChange = { viewModel.onEvent(ShoppingEvent.UpdateNewItemText(it)) },
                            label = { Text(Loc.shoplist_addItem) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                            enabled = uiState.newItemText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
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
    onChangeAisle: () -> Unit,
    userName: String
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CompactActionListItem(
        onClick = null, // Disable whole-item click for checkboxes
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
