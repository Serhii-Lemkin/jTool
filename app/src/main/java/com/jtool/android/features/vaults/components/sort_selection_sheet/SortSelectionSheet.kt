package com.jtool.android.features.vaults.components.sort_selection_sheet

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
import com.jtool.android.features.vaults.VaultSortEnum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSelectionSheet(
    currentSort: VaultSortEnum,
    onSelectSort: (VaultSortEnum) -> Unit,
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
                text = Loc.s("vaults_sortBy", "Sort Documents By"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ActionListItem(
                headline = Loc.s("vaults_sortUploadDate", "Upload Date (Newest First)"),
                supportingText = Loc.s("vaults_sortUploadDateSubtitle", "Default sort by cloud upload timestamp"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = if (currentSort == VaultSortEnum.UPLOAD_DATE_DESC) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {
                    onSelectSort(VaultSortEnum.UPLOAD_DATE_DESC)
                    onDismiss()
                }
            )

            ActionListItem(
                headline = Loc.s("vaults_sortName", "Document Name (A - Z)"),
                supportingText = Loc.s("vaults_sortNameSubtitle", "Alphabetical order by title"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.SortByAlpha,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = if (currentSort == VaultSortEnum.NAME_ASC) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {
                    onSelectSort(VaultSortEnum.NAME_ASC)
                    onDismiss()
                }
            )

            ActionListItem(
                headline = Loc.s("vaults_sortDocDate", "Document Date (Newest First)"),
                supportingText = Loc.s("vaults_sortDocDateSubtitle", "Sort by document issue date"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = if (currentSort == VaultSortEnum.DOC_DATE_DESC) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {
                    onSelectSort(VaultSortEnum.DOC_DATE_DESC)
                    onDismiss()
                }
            )

            ActionListItem(
                headline = Loc.s("vaults_sortTag", "Tag Name (A - Z)"),
                supportingText = Loc.s("vaults_sortTagSubtitle", "Alphabetical order by tag category"),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = if (currentSort == VaultSortEnum.TAG_ASC) {
                    { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                onClick = {
                    onSelectSort(VaultSortEnum.TAG_ASC)
                    onDismiss()
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
