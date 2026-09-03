package com.jtool.android.features.settings.groups.components.join_request_sent_slip

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRequestSentSlip(
    result: JoinGroupResultEnum,
    onDismiss: () -> Unit
) {
    val title = when (result) {
        JoinGroupResultEnum.ALREADY_REQUESTED -> Loc.settings_alreadyRequested
        JoinGroupResultEnum.ALREADY_MEMBER -> Loc.settings_alreadyMember
        else -> Loc.settings_joinRequestSent
    }
    val message = when (result) {
        JoinGroupResultEnum.ALREADY_REQUESTED -> Loc.settings_alreadyRequestedExpl
        JoinGroupResultEnum.ALREADY_MEMBER -> Loc.settings_alreadyMemberExpl
        else -> Loc.settings_joinRequestExplForRequester
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(Loc.general_close)
            }
        }
    }
}
