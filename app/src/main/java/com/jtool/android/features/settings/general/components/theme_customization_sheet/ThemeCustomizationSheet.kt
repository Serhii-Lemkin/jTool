package com.jtool.android.features.settings.general.components.theme_customization_sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jtool.android.core.localization.Loc
import com.jtool.android.ui.components.common.GenericSheet
import com.jtool.android.ui.theme.ThemeColorPalettes
import com.jtool.android.ui.theme.parseColorHex

@Composable
fun ThemeCustomizationSheet(
    isDarkMode: Boolean,
    currentPrimaryHex: String,
    currentBgHex: String,
    onSave: (primaryHex: String, bgHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPrimary by remember { mutableStateOf(currentPrimaryHex) }
    var selectedBg by remember { mutableStateOf(currentBgHex) }

    val primaryOptions = if (isDarkMode) ThemeColorPalettes.darkPrimaryOptions else ThemeColorPalettes.lightPrimaryOptions
    val bgOptions = if (isDarkMode) ThemeColorPalettes.darkBgOptions else ThemeColorPalettes.lightBgOptions

    val previewPrimary = parseColorHex(selectedPrimary, if (isDarkMode) Color(0xFF10B981) else Color(0xFF059669))
    val previewBg = parseColorHex(selectedBg, if (isDarkMode) Color(0xFF09090B) else Color(0xFFF0FDF4))
    val isDarkPreview = previewBg.luminance() < 0.5f

    val previewTitleColor = if (isDarkPreview) {
        Color.White
    } else {
        Color(
            red = (previewPrimary.red * 0.35f).coerceIn(0f, 1f),
            green = (previewPrimary.green * 0.35f).coerceIn(0f, 1f),
            blue = (previewPrimary.blue * 0.35f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    GenericSheet(
        onDismiss = onDismiss,
        title = if (isDarkMode) "Customize Dark Theme" else "Customize Light Theme",
        showCloseButton = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            // Live Preview Example Section
            Text(
                text = "Live Preview Example",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = previewBg,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sample Title Text",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = previewTitleColor
                    )
                    
                    // Action Item example
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkPreview) Color(0xFF18181B) else previewBg,
                        border = BorderStroke(1.dp, previewPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = previewPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = previewPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Sample Action Item",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkPreview) Color.White else Color(0xFF0F172A)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = previewPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Primary Accent Color",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            
            primaryOptions.chunked(5).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowItems.forEach { option ->
                        val isSelected = option.hex == selectedPrimary
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                                .clickable { selectedPrimary = option.hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                    repeat(5 - rowItems.size) {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Background Color",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            bgOptions.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { option ->
                        val isSelected = option.hex == selectedBg
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(option.color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedBg = option.hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (option.color.luminance() < 0.5f) Color.White else Color.Black, CircleShape)
                                )
                            }
                        }
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(selectedPrimary, selectedBg)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(Loc.General.save)
            }
        }
    }
}
