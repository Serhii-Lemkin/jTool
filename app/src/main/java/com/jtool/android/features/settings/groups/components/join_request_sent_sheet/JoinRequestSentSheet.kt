package com.jtool.android.features.settings.groups.components.join_request_sent_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.group.enums.JoinGroupResultEnum
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun JoinRequestSentSheet(
    result: JoinGroupResultEnum,
    onDismiss: () -> Unit
) {
    val titleStr = when (result) {
        JoinGroupResultEnum.ALREADY_REQUESTED -> Loc.Settings.alreadyRequested
        JoinGroupResultEnum.ALREADY_MEMBER -> Loc.Settings.alreadyMember
        else -> Loc.Settings.joinRequestSent
    }
    val messageStr = when (result) {
        JoinGroupResultEnum.ALREADY_REQUESTED -> Loc.Settings.alreadyRequestedExpl
        JoinGroupResultEnum.ALREADY_MEMBER -> Loc.Settings.alreadyMemberExpl
        else -> Loc.Settings.joinRequestExplForRequester
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = titleStr
    ) {
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = messageStr,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
