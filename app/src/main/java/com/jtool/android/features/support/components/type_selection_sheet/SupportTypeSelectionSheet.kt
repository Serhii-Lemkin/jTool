package com.jtool.android.features.support.components.type_selection_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import com.jtool.android.features.support.screens.SupportTypeEnumIcons
import com.jtool.android.ui.components.common.GenericSheet
import com.jtool.android.core.components.layout.ActionListItem

@Composable
fun SupportTypeSelectionSheet(
    onTypeSelected: (SupportTypeEnum) -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Support.new_request
    ) {
        SupportTypeEnum.entries.filter { it != SupportTypeEnum.CRASH }.forEach { type ->
            ActionListItem(
                leadingContent = { 
                    Icon(
                        imageVector = SupportTypeEnumIcons[type] ?: Icons.Default.Circle, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    ) 
                },
                headline = when (type) {
                    SupportTypeEnum.BUG -> Loc.Support.type_bug
                    SupportTypeEnum.FEATURE -> Loc.Support.type_feature
                    SupportTypeEnum.CHANGE -> Loc.Support.type_change
                    else -> ""
                },
                supportingText = when (type) {
                    SupportTypeEnum.BUG -> Loc.Support.type_bug_desc
                    SupportTypeEnum.FEATURE -> Loc.Support.type_feature_desc
                    SupportTypeEnum.CHANGE -> Loc.Support.type_change_desc
                    else -> ""
                },
                supportingMaxLines = Int.MAX_VALUE,
                onClick = {
                    onTypeSelected(type)
                    onDismiss()
                }
            )
        }
    }
}
