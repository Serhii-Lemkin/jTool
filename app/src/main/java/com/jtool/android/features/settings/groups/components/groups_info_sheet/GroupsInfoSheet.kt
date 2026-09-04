package com.jtool.android.features.settings.groups.components.groups_info_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun GroupsInfoSheet(
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Settings.groupsInfoTitle,
        showCloseButton = true
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            InfoStep(
                title = Loc.Settings.groupsInfoStep1Title,
                desc = Loc.Settings.groupsInfoStep1Desc
            )
            Spacer(Modifier.height(24.dp))
            InfoStep(
                title = Loc.Settings.groupsInfoStep2Title,
                desc = Loc.Settings.groupsInfoStep2Desc
            )
            Spacer(Modifier.height(24.dp))
            InfoStep(
                title = Loc.Settings.groupsInfoStep3Title,
                desc = Loc.Settings.groupsInfoStep3Desc
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoStep(title: String, desc: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
