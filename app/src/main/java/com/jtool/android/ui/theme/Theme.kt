package com.jtool.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun JToolTheme(
    darkTheme: Boolean = true,
    lightPrimary: String = "0xFF059669",
    lightBg: String = "0xFFF0FDF4",
    darkPrimary: String = "0xFF10B981",
    darkBg: String = "0xFF09090B",
    content: @Composable () -> Unit
) {
    val primaryColor = parseColorHex(if (darkTheme) darkPrimary else lightPrimary, if (darkTheme) Color(0xFF10B981) else Color(0xFF059669))
    val bgColor = parseColorHex(if (darkTheme) darkBg else lightBg, if (darkTheme) Color(0xFF09090B) else Color(0xFFF0FDF4))
    
    val isDarkBg = bgColor.luminance() < 0.5f

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            background = bgColor,
            onBackground = if (isDarkBg) Color(0xFFF4F4F5) else Color(0xFF0F172A),
            surface = if (isDarkBg) Color(0xFF18181B) else bgColor,
            onSurface = if (isDarkBg) Color(0xFFF4F4F5) else Color(0xFF0F172A),
            surfaceVariant = if (isDarkBg) Color(0xFF27272A) else Color(0xFF334155),
            onSurfaceVariant = if (isDarkBg) Color(0xFFA1A1AA) else Color(0xFF94A3B8)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor,
            background = bgColor,
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
