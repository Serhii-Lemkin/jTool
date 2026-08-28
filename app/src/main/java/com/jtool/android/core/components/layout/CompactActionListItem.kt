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
fun CompactActionListItem(
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

    val rowModifier = if (onClick != null || onLongClick != null) {
        modifier.combinedClickable(
            onClick = { if (!isPending) onClick?.invoke() },
            onLongClick = { if (!isPending) onLongClick?.invoke() }
        )
    } else {
        modifier
    }

    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(modifier = Modifier.padding(end = 2.dp)) {
                leadingContent()
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 4.dp, top = 2.dp, bottom = 2.dp)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = headlineColor.copy(alpha = if (isStrikethrough) 0.6f else alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = supportingColor.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (metadataText != null) {
                Text(
                    text = metadataText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = metadataColor.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingContent != null) {
            Box(modifier = Modifier.padding(start = 2.dp)) {
                trailingContent()
            }
        }
    }
}
