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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jtool.android.core.backup.BackupManager
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.settings.SettingsManager
import com.jtool.android.core.sync.GroupSyncManager
import kotlinx.coroutines.launch

val AisleIcons = mapOf(
    "Common" to Icons.AutoMirrored.Filled.Label,
    "Veggies" to Icons.Default.Eco,
    "Meat" to Icons.Default.SetMeal,
    "Dairy" to Icons.Default.Egg,
    "Bakery" to Icons.Default.BakeryDining,
    "Drinks" to Icons.Default.LocalDrink,
    "Frozen" to Icons.Default.AcUnit,
    "Snacks" to Icons.Default.Cookie,
    "Household" to Icons.Default.CleaningServices,
    "Personal" to Icons.Default.Spa,
    "Pets" to Icons.Default.Pets
)

@Composable
fun ShoppingListScreen(
    manager: ShoppingListManager,
    backupManager: BackupManager,
    settingsManager: SettingsManager,
    groupSyncManager: GroupSyncManager,
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
            onDismissRequest = { viewModel.onEvent(ShoppingEvent.ToggleAislePicker(false)) },
            title = { Text("Select Aisle") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AisleIcons.toList()) { (name, icon) ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    viewModel.onEvent(ShoppingEvent.SelectAisle(name))
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (uiState.selectedAisle == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
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
            title = { Text(Loc.confirmDeleteTitle) },
            text = { Text(Loc.confirmDeleteMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(ShoppingEvent.ConfirmDelete)
                }) {
                    Text(Loc.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ShoppingEvent.RequestDelete(null)) }) {
                    Text(Loc.cancel)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Loc.shoppingList,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box {
                        IconButton(onClick = { viewModel.onEvent(ShoppingEvent.ToggleFilterDropdown(true)) }) {
                            val icon = when {
                                uiState.filterType == "Unactive" -> Icons.Default.CheckBox
                                uiState.filterType.startsWith("Aisle:") -> AisleIcons[uiState.filterType.removePrefix("Aisle:")] ?: Icons.AutoMirrored.Filled.Label
                                else -> Icons.Default.FilterList
                            }
                            Icon(icon, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(
                            expanded = uiState.dropdownExpanded,
                            onDismissRequest = { viewModel.onEvent(ShoppingEvent.ToggleFilterDropdown(false)) }
                        ) {
                            DropdownMenuItem(
                                text = { Icon(Icons.Default.FilterList, contentDescription = Loc.all, tint = MaterialTheme.colorScheme.primary) },
                                onClick = { viewModel.onEvent(ShoppingEvent.UpdateFilter("All")) }
                            )
                            DropdownMenuItem(
                                text = { Icon(Icons.Default.CheckBox, contentDescription = Loc.unactive, tint = MaterialTheme.colorScheme.primary) },
                                onClick = { viewModel.onEvent(ShoppingEvent.UpdateFilter("Unactive")) }
                            )
                            uiState.activeAisles.forEach { aisle ->
                                DropdownMenuItem(
                                    text = { Icon(AisleIcons[aisle] ?: Icons.AutoMirrored.Filled.Label, contentDescription = aisle, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { viewModel.onEvent(ShoppingEvent.UpdateFilter("Aisle:$aisle")) }
                                )
                            }
                        }
                    }
                }
            }

            items(uiState.displayItems, key = { it.id }) { item ->
                ShoppingItemRow(
                    item = item,
                    onToggle = { checked ->
                        viewModel.onEvent(ShoppingEvent.ToggleItem(item, checked))
                    },
                    onDelete = {
                        viewModel.onEvent(ShoppingEvent.RequestDelete(item))
                    },
                    userName = uiState.userNames[item.lastUpdatedBy] ?: item.lastUpdatedBy
                )
            }

            if (uiState.checkedItems.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = Loc.recentlyChecked,
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
                        userName = uiState.userNames[item.lastUpdatedBy] ?: item.lastUpdatedBy
                    )
                }
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
                                    text = Loc.restore,
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
            modifier = Modifier.fillMaxWidth(),
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
                label = { Text(Loc.addItem) },
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
                            Toast.makeText(context, Loc.itemAlreadyInList, Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.onEvent(ShoppingEvent.AddItem)
                            if (existingItem != null && existingItem.isChecked) {
                                Toast.makeText(context, Loc.restoredExisting, Toast.LENGTH_SHORT).show()
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

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    userName: String // Pass the resolved name
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp), // Shrink padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = onToggle
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.text,
                        fontSize = 16.sp,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isChecked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            AisleIcons[item.aisle] ?: Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${Loc.by} $userName",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ShoppingListPreview() {
    val context = LocalContext.current
    MaterialTheme {
        ShoppingListScreen(
            manager = ShoppingListManager(context),
            backupManager = BackupManager(context),
            settingsManager = SettingsManager(context),
            groupSyncManager = GroupSyncManager(context)
        )
    }
}
