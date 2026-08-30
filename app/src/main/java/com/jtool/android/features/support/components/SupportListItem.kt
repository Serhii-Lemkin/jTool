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
import com.jtool.android.core.models.support.SupportRequest
import com.jtool.android.core.models.support.enums.SupportStatusEnum
import com.jtool.android.core.models.support.enums.SupportTypeEnum
import com.jtool.android.core.components.layout.ActionListItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SupportListItem(
    request: SupportRequest,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    
    ActionListItem(
        onClick = onClick,
        headline = Loc.support_request_number.format(request.number),
        headlineColor = MaterialTheme.colorScheme.primary,
        supportingText = request.text,
        supportingColor = MaterialTheme.colorScheme.onSurface,
        metadataText = buildString {
            append(dateFormat.format(Date(request.timestamp)))
            append(" • ")
            append(request.userName)
            if (request.commentCount > 0) {
                append(" • ")
                append(request.commentCount)
                append(" 💬")
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                TypeLabel(request.pendingType ?: request.type, isPending = request.pendingType != null)
                Spacer(Modifier.height(4.dp))
                StatusBadge(request.pendingStatus ?: request.status, isPending = request.pendingStatus != null)
            }
        }
    )
}

@Composable
fun TypeLabel(type: SupportTypeEnum, isPending: Boolean = false) {
    val text = when (type) {
        SupportTypeEnum.BUG -> Loc.support_type_bug
        SupportTypeEnum.FEATURE -> Loc.support_type_feature
        SupportTypeEnum.CHANGE -> Loc.support_type_change
        SupportTypeEnum.CRASH -> Loc.support_type_crash
    }
    Text(
        text = text.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isPending) 0.3f else 0.5f)
    )
}

@Composable
fun StatusBadge(status: SupportStatusEnum, isPending: Boolean = false) {
    val (text, color) = when (status) {
        SupportStatusEnum.OPENED -> Loc.support_status_opened to MaterialTheme.colorScheme.error
        SupportStatusEnum.IN_PROGRESS -> Loc.support_status_in_progress to MaterialTheme.colorScheme.primary
        SupportStatusEnum.CLOSED -> Loc.support_status_closed to MaterialTheme.colorScheme.secondary
        SupportStatusEnum.CUT -> Loc.support_status_cut to MaterialTheme.colorScheme.outline
    }
    
    Surface(
        color = color.copy(alpha = if (isPending) 0.05f else 0.1f),
        contentColor = color.copy(alpha = if (isPending) 0.4f else 1.0f),
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
