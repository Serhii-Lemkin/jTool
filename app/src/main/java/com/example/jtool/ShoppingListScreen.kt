package com.example.jtool

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
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
    groupSyncManager: GroupSyncManager
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(manager.loadItems()) }
    var newItemText by remember { mutableStateOf("") }
    var selectedAisle by remember { mutableStateOf("Common") }
    var showAislePicker by remember { mutableStateOf(false) }
    var sortByAisle by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentGroupId = remember { settingsManager.getCurrentGroupId() }
    val syncUri = remember { settingsManager.getSyncUri()?.let { Uri.parse(it) } }

    val displayItems = if (sortByAisle) {
        items.filter { !it.isChecked }.sortedWith(compareBy({ it.aisle }, { it.text }))
    } else {
        items.filter { !it.isChecked }.sortedByDescending { it.id }
    }

    val checkedItems = items.filter { it.isChecked }.sortedByDescending { it.timestamp }.take(5)
    
    // Remote suggestions from Firebase
    val remoteSuggestions by groupSyncManager.suggestionsFlow().collectAsState(initial = emptyList())
    val userNames by groupSyncManager.userNamesFlow().collectAsState(initial = emptyMap()) // Added this

    val suggestions = if (newItemText.isNotBlank()) {
        (items.filter { it.isChecked && it.text.startsWith(newItemText, ignoreCase = true) }.map { it.text } +
         remoteSuggestions.filter { it.startsWith(newItemText, ignoreCase = true) })
            .distinct()
            .take(5)
    } else {
        emptyList()
    }

    fun triggerSync() {
        settingsManager.setSyncPending(true)
        scope.launch {
            groupSyncManager.pushGroupData()
        }
    }

    fun updateItems(newList: List<ShoppingItem>) {
        items = newList
        manager.saveItems(newList)
        triggerSync()
    }

    // Real-time Firebase Sync
    LaunchedEffect(currentGroupId) {
        if (currentGroupId != null) {
            groupSyncManager.groupDataFlow(currentGroupId).collect {
                items = manager.loadItems()
            }
        }
    }

    // Initial Load & Cleanup
    LaunchedEffect(Unit) {
        items = manager.loadItems()
    }

    if (showAislePicker) {
        AlertDialog(
            onDismissRequest = { showAislePicker = false },
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
                                    selectedAisle = name
                                    showAislePicker = false
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (selectedAisle == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {}
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
            verticalArrangement = Arrangement.spacedBy(0.dp) // Shrink vertical spacing
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(Loc.sortByAisle, fontSize = 12.sp)
                    Checkbox(checked = sortByAisle, onCheckedChange = { sortByAisle = it })
                }
            }

            items(displayItems, key = { it.id }) { item ->
                ShoppingItemRow(
                    item = item,
                    onToggle = { checked ->
                        val updated = items.map {
                            if (it.id == item.id) it.copy(
                                isChecked = checked, 
                                timestamp = System.currentTimeMillis(),
                                lastUpdatedBy = settingsManager.getAppUserId() // Use ID
                            ) else it
                        }
                        updateItems(updated)
                    },
                    onDelete = {
                        updateItems(items.filter { it.id != item.id })
                    },
                    userName = userNames[item.lastUpdatedBy] ?: item.lastUpdatedBy
                )
            }

            if (checkedItems.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = Loc.recentlyChecked,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                items(checkedItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onToggle = { checked ->
                            val updated = items.map {
                                if (it.id == item.id) it.copy(
                                    isChecked = checked, 
                                    timestamp = System.currentTimeMillis(),
                                    lastUpdatedBy = settingsManager.getAppUserId() // Use ID
                                ) else it
                            }
                            updateItems(updated)
                        },
                        onDelete = {
                            updateItems(items.filter { it.id != item.id })
                        },
                        userName = userNames[item.lastUpdatedBy] ?: item.lastUpdatedBy
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Column {
                    suggestions.forEach { suggestionText ->
                        TextButton(
                            onClick = {
                                val existing = items.find { it.text.equals(suggestionText, ignoreCase = true) }
                                val updated = if (existing != null) {
                                    items.map {
                                        if (it.id == existing.id) it.copy(
                                            isChecked = false, 
                                            timestamp = System.currentTimeMillis(),
                                            lastUpdatedBy = settingsManager.getAppUserId() // Use ID
                                        ) else it
                                    }
                                } else {
                                    val newItem = ShoppingItem(
                                        id = System.currentTimeMillis(),
                                        text = suggestionText,
                                        isChecked = false,
                                        lastUpdatedBy = settingsManager.getAppUserId(), // Use ID
                                        aisle = selectedAisle
                                    )
                                    items + newItem
                                }
                                updateItems(updated)
                                newItemText = ""
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
            IconButton(onClick = { showAislePicker = true }) {
                Icon(
                    AisleIcons[selectedAisle] ?: Icons.AutoMirrored.Filled.Label,
                    contentDescription = "Select Aisle",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                label = { Text(Loc.addItem) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        val trimmedText = newItemText.trim()
                        val existingItem = items.find { it.text.equals(trimmedText, ignoreCase = true) }
                        
                        if (existingItem != null) {
                            if (existingItem.isChecked) {
                                val updated = items.map {
                                    if (it.id == existingItem.id) it.copy(isChecked = false, timestamp = System.currentTimeMillis()) else it
                                }
                                updateItems(updated)
                                newItemText = ""
                                Toast.makeText(context, Loc.restoredExisting, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, Loc.itemAlreadyInList, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val newItem = ShoppingItem(
                                id = System.currentTimeMillis(),
                                text = trimmedText,
                                isChecked = false,
                                lastUpdatedBy = settingsManager.getAppUserId(), // Use ID
                                aisle = selectedAisle
                            )
                            updateItems(items + newItem)
                            scope.launch { groupSyncManager.pushSuggestion(trimmedText) }
                            newItemText = ""
                            selectedAisle = "Common"
                        }
                    }
                },
                enabled = newItemText.isNotBlank()
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
                    if (item.aisle != "Common" && !item.isChecked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                AisleIcons[item.aisle] ?: Icons.Default.Label,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = "${Loc.by} $userName", // Use passed name
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.End).padding(end = 12.dp)
            )
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
