package com.jtool.android.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SimpleColorPicker(
    selectedColor: Long?,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = remember {
        listOf(
            0xFF4285F4, 0xFFEA4335, 0xFFFBBC04, 0xFF34A853, 
            0xFF8E24AA, 0xFFF06292, 0xFF4DB6AC, 0xFFFF7043,
            0xFF5C6BC0, 0xFF7E57C2, 0xFF26A69A, 0xFF9CCC65
        )
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(colors) { colorArgb ->
            val color = Color(colorArgb)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 2.dp,
                        color = if (selectedColor == colorArgb) Color.Black.copy(alpha = 0.5f) else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorArgb) }
            )
        }
    }
}
