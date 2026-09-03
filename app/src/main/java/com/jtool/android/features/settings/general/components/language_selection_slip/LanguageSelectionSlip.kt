package com.jtool.android.features.settings.general.components.language_selection_slip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.LanguageMetadata
import com.jtool.android.core.localization.Loc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSlip(
    languages: List<LanguageMetadata>,
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchVisible) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(Loc.general_search) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { 
                                isSearchVisible = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    )
                } else {
                    Text(
                        text = Loc.settings_language,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isSearchVisible = true }) {
                        Icon(Icons.Default.Search, null)
                    }
                }
            }
            
            val filteredLanguages = languages.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || it.shortCode.contains(searchQuery, ignoreCase = true)
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
            ) {
                items(filteredLanguages) { lang ->
                    ActionListItem(
                        headline = lang.name,
                        supportingText = lang.shortCode.uppercase(),
                        onClick = { onLanguageSelected(lang.locale); onDismiss() },
                        trailingContent = if (lang.locale == currentLanguageCode) {
                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            
            if (filteredLanguages.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(Loc.worktime_noDataFound, color = Color.Gray)
                }
            }
        }
    }
}
