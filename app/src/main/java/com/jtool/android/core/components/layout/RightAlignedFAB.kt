package com.jtool.android.core.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.dp

@Composable
fun RightAlignedFAB(
    content: @Composable () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    // In LTR, End is Right. In RTL, Start is Right.
    val alignment = if (layoutDirection == LayoutDirection.Rtl) Alignment.BottomStart else Alignment.BottomEnd
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = alignment
    ) {
        content()
    }
}
