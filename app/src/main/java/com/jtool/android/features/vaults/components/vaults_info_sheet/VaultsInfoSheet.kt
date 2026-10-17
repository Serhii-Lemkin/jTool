package com.jtool.android.features.vaults.components.vaults_info_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun VaultsInfoSheet(onDismiss: () -> Unit) {
    GenericSheet(
        onDismiss = onDismiss,
        title = VaultsLoc.vaultsInfoTitle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = VaultsLoc.vaultsInfoDesc,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
