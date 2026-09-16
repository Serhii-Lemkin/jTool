package com.jtool.android.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemeColorOption(
    val color: Color,
    val hex: String
)

object ThemeColorPalettes {
    val lightPrimaryOptions = listOf(
        ThemeColorOption(Color(0xFF059669), "0xFF059669"), // 1. Emerald
        ThemeColorOption(Color(0xFF4F46E5), "0xFF4F46E5"), // 2. Indigo
        ThemeColorOption(Color(0xFF7C3AED), "0xFF7C3AED"), // 3. Purple
        ThemeColorOption(Color(0xFF2563EB), "0xFF2563EB"), // 4. Blue
        ThemeColorOption(Color(0xFF0D9488), "0xFF0D9488"), // 5. Teal
        ThemeColorOption(Color(0xFFD97706), "0xFFD97706"), // 6. Amber
        ThemeColorOption(Color(0xFFE11D48), "0xFFE11D48"), // 7. Rose
        ThemeColorOption(Color(0xFF0891B2), "0xFF0891B2"), // 8. Cyan
        ThemeColorOption(Color(0xFFC026D3), "0xFFC026D3"), // 9. Fuchsia
        ThemeColorOption(Color(0xFFEA580C), "0xFFEA580C"), // 10. Orange
        ThemeColorOption(Color(0xFF475569), "0xFF475569"), // 11. Slate
        ThemeColorOption(Color(0xFF15803D), "0xFF15803D"), // 12. Forest
        ThemeColorOption(Color(0xFFB45309), "0xFFB45309"), // 13. Bronze
        ThemeColorOption(Color(0xFF6D28D9), "0xFF6D28D9"), // 14. Deep Violet
        ThemeColorOption(Color(0xFFBE123C), "0xFFBE123C")  // 15. Ruby
    )

    val lightBgOptions = listOf(
        ThemeColorOption(Color(0xFFF0FDF4), "0xFFF0FDF4"), // 1. Soft Sage
        ThemeColorOption(Color(0xFFFFFBEB), "0xFFFFFBEB"), // 2. Warm Cream
        ThemeColorOption(Color(0xFFF8FAFC), "0xFFF8FAFC"), // 3. Cool Gray
        ThemeColorOption(Color(0xFFFFFFFF), "0xFFFFFFFF"), // 4. Clean White
        ThemeColorOption(Color(0xFFEFF6FF), "0xFFEFF6FF"), // 5. Soft Blue
        ThemeColorOption(Color(0xFFFFF1F2), "0xFFFFF1F2")  // 6. Soft Rose
    )

    val darkPrimaryOptions = listOf(
        ThemeColorOption(Color(0xFF10B981), "0xFF10B981"), // 1. Mint
        ThemeColorOption(Color(0xFFA855F7), "0xFFA855F7"), // 2. Purple
        ThemeColorOption(Color(0xFF38BDF8), "0xFF38BDF8"), // 3. Sky
        ThemeColorOption(Color(0xFFFB923C), "0xFFFB923C"), // 4. Orange
        ThemeColorOption(Color(0xFFF43F5E), "0xFFF43F5E"), // 5. Rose
        ThemeColorOption(Color(0xFF84CC16), "0xFF84CC16"), // 6. Lime
        ThemeColorOption(Color(0xFF2DD4BF), "0xFF2DD4BF"), // 7. Teal
        ThemeColorOption(Color(0xFFFACC15), "0xFFFACC15"), // 8. Yellow
        ThemeColorOption(Color(0xFF8B5CF6), "0xFF8B5CF6"), // 9. Violet
        ThemeColorOption(Color(0xFFEC4899), "0xFFEC4899"), // 10. Pink
        ThemeColorOption(Color(0xFF6EE7B7), "0xFF6EE7B7"), // 11. Light Mint
        ThemeColorOption(Color(0xFF93C5FD), "0xFF93C5FD"), // 12. Light Blue
        ThemeColorOption(Color(0xFFFDBA74), "0xFFFDBA74"), // 13. Light Orange
        ThemeColorOption(Color(0xFFFB7185), "0xFFFB7185"), // 14. Coral
        ThemeColorOption(Color(0xFF60A5FA), "0xFF60A5FA")  // 15. Electric Blue
    )

    val darkBgOptions = listOf(
        ThemeColorOption(Color(0xFF09090B), "0xFF09090B"), // 1. Obsidian
        ThemeColorOption(Color(0xFF0F172A), "0xFF0F172A"), // 2. Navy
        ThemeColorOption(Color(0xFF1E293B), "0xFF1E293B"), // 3. Slate
        ThemeColorOption(Color(0xFF121212), "0xFF121212"), // 4. Charcoal
        ThemeColorOption(Color(0xFF022C22), "0xFF022C22"), // 5. Forest
        ThemeColorOption(Color(0xFF4C0519), "0xFF4C0519")  // 6. Wine
    )
}

fun parseColorHex(hex: String, default: Color): Color {
    return try {
        val clean = hex.removePrefix("0x").removePrefix("0X")
        val colorLong = clean.toLong(16)
        Color(colorLong)
    } catch (_: Exception) {
        default
    }
}
