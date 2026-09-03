package com.jtool.android.features.settings.groups.components.group_member_item_slip

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc

@Composable
fun GroupMemberItem(
    name: String,
    isOwner: Boolean
) {
    ActionListItem(
        headline = name,
        leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = if (isOwner) {
            {
                SuggestionChip(
                    onClick = { },
                    label = { Text(Loc.settings_ownerLabel, fontSize = 10.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = null
                )
            }
        } else null
    )
}
