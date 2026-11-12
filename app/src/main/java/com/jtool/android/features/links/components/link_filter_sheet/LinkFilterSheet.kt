package com.jtool.android.features.links.components.link_filter_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.LinksLoc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkFilterSheet(
    allTags: List<Pair<String, String>>,
    selectedTags: Set<String>,
    onApplyFilters: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSelectedTags by remember(selectedTags) { mutableStateOf(selectedTags) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        currentSelectedTags = allTags.map { it.first }.toSet()
                    }
                ) {
                    Text(Loc.s("general_selectAll", "Select All"))
                }

                TextButton(
                    onClick = {
                        currentSelectedTags = emptySet()
                    }
                ) {
                    Text(Loc.s("general_clearAll", "Clear All"))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allTags.forEach { (key, label) ->
                    val isSelected = currentSelectedTags.contains(key)
                    ActionListItem(
                        headline = label,
                        trailingContent = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            currentSelectedTags = if (isSelected) {
                                currentSelectedTags - key
                            } else {
                                currentSelectedTags + key
                            }
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

            Button(
                onClick = {
                    onApplyFilters(currentSelectedTags)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(Loc.s("vaults_applyFilters", "Apply"), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
