package com.jtool.android.core.components.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionListItem(
    modifier: Modifier = Modifier,
    headline: String,
    supportingText: String? = null,
    metadataText: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isPending: Boolean = false,
    isStrikethrough: Boolean = false,
    headlineColor: Color = MaterialTheme.colorScheme.onSurface,
    supportingColor: Color = MaterialTheme.colorScheme.secondary,
    metadataColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
) {
    val alpha = if (isPending) 0.5f else 1.0f
    
    ListItem(
        modifier = modifier
            .combinedClickable(
                onClick = { if (!isPending) onClick?.invoke() },
                onLongClick = { if (!isPending) onLongClick?.invoke() }
            ),
        headlineContent = {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = headlineColor.copy(alpha = if (isStrikethrough) 0.6f else alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            if (supportingText != null || metadataText != null) {
                Column {
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = supportingColor.copy(alpha = alpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (metadataText != null) {
                        Text(
                            text = metadataText,
                            style = MaterialTheme.typography.labelSmall,
                            color = metadataColor.copy(alpha = alpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}
