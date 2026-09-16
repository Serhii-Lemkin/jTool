package com.jtool.android.features.support.components.reviewer_info_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun ReviewerInfoSheet(
    onDismiss: () -> Unit
) {
    GenericSheet(
        onDismiss = onDismiss,
        title = "Reviewer Status",
        showCloseButton = false
    ) {
        Icon(
            Icons.Default.RateReview,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "You are a designated Reviewer. You have unlimited support request submissions to help evaluate and improve the application. Thank you for your valuable contribution to making jTool better.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
    }
}
