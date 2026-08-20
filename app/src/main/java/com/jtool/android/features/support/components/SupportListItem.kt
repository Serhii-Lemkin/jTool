package com.jtool.android.features.support.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.features.support.logic.SupportRequest
import com.jtool.android.features.support.logic.SupportStatus
import com.jtool.android.features.support.logic.SupportType
import com.jtool.android.core.components.comments.getTotalCommentsCount
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SupportListItem(
    request: SupportRequest,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Loc.support_request_number.format(request.number),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = request.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateFormat.format(Date(request.timestamp)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = request.userName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                val commentCount = getTotalCommentsCount(request.comments)
                if (commentCount > 0) {
                    Spacer(Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = commentCount.toString(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                TypeLabel(request.type)
                Spacer(Modifier.height(4.dp))
                StatusBadge(request.status)
            }
        }
    )
}

@Composable
fun TypeLabel(type: SupportType) {
    val text = when (type) {
        SupportType.BUG -> Loc.support_type_bug
        SupportType.FEATURE -> Loc.support_type_feature
        SupportType.CHANGE -> Loc.support_type_change
        SupportType.CRASH -> Loc.support_type_crash
    }
    Text(
        text = text.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun StatusBadge(status: SupportStatus) {
    val (text, color) = when (status) {
        SupportStatus.OPENED -> Loc.support_status_opened to MaterialTheme.colorScheme.error
        SupportStatus.IN_PROGRESS -> Loc.support_status_in_progress to MaterialTheme.colorScheme.primary
        SupportStatus.CLOSED -> Loc.support_status_closed to MaterialTheme.colorScheme.secondary
        SupportStatus.CUT -> Loc.support_status_cut to MaterialTheme.colorScheme.outline
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
