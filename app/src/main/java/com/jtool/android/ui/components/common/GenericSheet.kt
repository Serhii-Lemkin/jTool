package com.jtool.android.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    showCloseButton: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = horizontalAlignment
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }

            content()

            if (showCloseButton) {
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Loc.General.close)
                }
            }
        }
    }
}
