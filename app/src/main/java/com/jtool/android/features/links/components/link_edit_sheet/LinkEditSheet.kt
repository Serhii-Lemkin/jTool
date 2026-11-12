package com.jtool.android.features.links.components.link_edit_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.LinksLoc
import com.jtool.android.ui.components.common.GenericSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkEditSheet(
    sheetTitle: String = Loc.Links.addLink,
    buttonText: String = Loc.General.add,
    title: String,
    url: String,
    tag: String = "#COMMON#",
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTagChange: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val allTags = remember { LinksLoc.allStandardLinkTags }
    var tagInput by remember(tag) { mutableStateOf(LinksLoc.getLocalizedLinkTag(tag)) }

    val filteredTags = remember(tagInput, allTags) {
        if (tagInput.isBlank()) {
            allTags
        } else {
            val lower = tagInput.trim().lowercase()
            allTags.filter { it.second.lowercase().contains(lower) || it.first.lowercase().contains(lower) }
        }
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = sheetTitle,
        showCloseButton = false
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(Loc.Links.linkTitle) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text(Loc.Links.linkUrl) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            placeholder = { Text(Loc.Links.urlHint) }
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (filteredTags.isNotEmpty()) {
                Text(
                    text = LinksLoc.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredTags, key = { it.first }) { (key, label) ->
                        val isSelected = tag.equals(key, ignoreCase = true) || tagInput.equals(label, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onTagChange(key)
                                tagInput = label
                            },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }

            OutlinedTextField(
                value = tagInput,
                onValueChange = { input ->
                    tagInput = input
                    val matchedKey = allTags.firstOrNull { it.second.equals(input.trim(), ignoreCase = true) }?.first
                    onTagChange(matchedKey ?: input.trim())
                },
                label = { Text(LinksLoc.tag) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                trailingIcon = {
                    if (tagInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                tagInput = ""
                                onTagChange("#COMMON#")
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            enabled = title.isNotBlank() && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(buttonText)
        }
    }
}
