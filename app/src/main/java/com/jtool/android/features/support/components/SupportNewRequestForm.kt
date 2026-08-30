package com.jtool.android.features.support.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.support.enums.SupportTypeEnum

@Composable
fun SupportNewRequestForm(
    text: String,
    onTextChange: (String) -> Unit,
    isSending: Boolean,
    selectedType: SupportTypeEnum,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text = Loc.support_new_request, style = MaterialTheme.typography.titleLarge)
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = when (selectedType) {
                        SupportTypeEnum.BUG -> Loc.support_type_bug
                        SupportTypeEnum.FEATURE -> Loc.support_type_feature
                        SupportTypeEnum.CHANGE -> Loc.support_type_change
                        SupportTypeEnum.CRASH -> Loc.support_type_crash
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text(Loc.support_enter_details) },
            enabled = !isSending
        )
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isSending && text.isNotBlank()
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(text = Loc.support_submit)
            }
        }
    }
}
