package com.jtool.android.features.vaults.components.vaults_search_option_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.components.layout.ActionListItem
import com.jtool.android.core.localization.Loc

enum class VaultSearchMode {
    VAULTS_ONLY,
    DOCS_ACROSS_VAULTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultsSearchOptionSheet(
    onSelectMode: (VaultSearchMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = Loc.s("vaults_searchOptionTitle", "Vault Search"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ActionListItem(
                headline = Loc.s("vaults_searchVaultsOption", "Search Vaults"),
                supportingText = Loc.s("vaults_searchVaultsOptionSubtitle", "Search vaults by name or description"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    onSelectMode(VaultSearchMode.VAULTS_ONLY)
                    onDismiss()
                }
            )

            ActionListItem(
                headline = Loc.s("vaults_searchDocsOption", "Search Documents Across Vaults"),
                supportingText = Loc.s("vaults_searchDocsOptionSubtitle", "Search documents, tags, dates, and descriptions across all group vaults"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.FindInPage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    onSelectMode(VaultSearchMode.DOCS_ACROSS_VAULTS)
                    onDismiss()
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
