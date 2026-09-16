package com.jtool.android.features.calendar.edit.components.event_color_picker_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.core.localization.domains.CalendarLoc
import com.jtool.android.ui.components.common.GenericSheet

data class CalendarColorOption(
    val key: String,
    val hexColor: Color,
    val name: String
)

@Composable
fun getGoogleCalendarColorPalette(): List<CalendarColorOption> = listOf(
    CalendarColorOption("7", Color(0xFF039BE5), CalendarLoc.colorPeacock),
    CalendarColorOption("2", Color(0xFF7CB342), CalendarLoc.colorSage),
    CalendarColorOption("3", Color(0xFF8E24AA), CalendarLoc.colorGrape),
    CalendarColorOption("4", Color(0xFFE67C73), CalendarLoc.colorFlamingo),
    CalendarColorOption("5", Color(0xFFF6BF26), CalendarLoc.colorBanana),
    CalendarColorOption("6", Color(0xFFF4511E), CalendarLoc.colorTangerine),
    CalendarColorOption("8", Color(0xFF616161), CalendarLoc.colorGraphite),
    CalendarColorOption("9", Color(0xFF3F51B5), CalendarLoc.colorBlueberry),
    CalendarColorOption("10", Color(0xFF0B8043), CalendarLoc.colorBasil),
    CalendarColorOption("11", Color(0xFFD50000), CalendarLoc.colorTomato)
)

fun getEventColor(colorKey: String?): Color {
    val key = colorKey ?: "7"
    return when (key) {
        "1", "7" -> Color(0xFF039BE5)
        "2" -> Color(0xFF7CB342)
        "3" -> Color(0xFF8E24AA)
        "4" -> Color(0xFFE67C73)
        "5" -> Color(0xFFF6BF26)
        "6" -> Color(0xFFF4511E)
        "8" -> Color(0xFF616161)
        "9" -> Color(0xFF3F51B5)
        "10" -> Color(0xFF0B8043)
        "11" -> Color(0xFFD50000)
        else -> Color(0xFF039BE5)
    }
}

@Composable
fun EventColorPickerSheet(
    selectedColorKey: String,
    onSelectColor: (CalendarColorOption) -> Unit,
    onDismiss: () -> Unit
) {
    val colorPalette = getGoogleCalendarColorPalette()

    GenericSheet(
        onDismiss = onDismiss,
        title = Loc.Calendar.eventColor,
        showCloseButton = false
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            items(colorPalette, key = { it.key }) { colorOption ->
                val isSelected = selectedColorKey == colorOption.key || (selectedColorKey.isEmpty() && colorOption.key == "7")

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorOption.hexColor)
                        .clickable {
                            onSelectColor(colorOption)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
