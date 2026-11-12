package com.jtool.android.features.shopping.components.shopping_item_action_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun ShoppingItemActionSheet(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = null,
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionListItem(
                headline = Loc.ShopList.editItem,
                supportingText = Loc.s("shopping_editItemDesc", "Edit name, amount, unit, or category"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onEdit()
                    onDismiss()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            ActionListItem(
                headline = Loc.General.delete,
                supportingText = Loc.s("shopping_deleteItemDesc", "Remove item from list"),
                headlineColor = MaterialTheme.colorScheme.error,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
