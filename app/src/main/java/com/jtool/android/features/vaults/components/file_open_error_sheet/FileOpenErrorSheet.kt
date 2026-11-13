package com.jtool.android.features.vaults.components.file_open_error_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.VaultsLoc
import com.jtool.android.core.models.vaults.enums.FileOpenErrorReasonEnum
import com.jtool.android.ui.components.common.GenericSheet

@Composable
fun FileOpenErrorSheet(
    reason: FileOpenErrorReasonEnum,
    onDismiss: () -> Unit
) {
    val reasonText = when (reason) {
        FileOpenErrorReasonEnum.MISSING_DRIVE_LINK -> VaultsLoc.openErrorMissingDriveLink
        FileOpenErrorReasonEnum.LOCAL_FILE_MISSING -> VaultsLoc.openErrorLocalMissing
        FileOpenErrorReasonEnum.INVALID_URL -> VaultsLoc.openErrorInvalidUrl
        FileOpenErrorReasonEnum.NO_APP_FOUND -> VaultsLoc.openErrorNoApp
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Vaults.openFileError
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = reasonText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(Loc.General.gotIt, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
