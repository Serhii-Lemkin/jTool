package com.jtool.android.features.shopping.components.item_rename_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun ItemRenameSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.ShopList.renameItem,
        showCloseButton = false
    ) {
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text(Loc.ShopList.itemNameLabel) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onConfirm(newName) },
            enabled = newName.isNotBlank() && newName != currentName,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(Loc.General.save)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(Loc.General.cancel)
        }
    }
}
