package com.jtool.android.ui.components.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.jtool.android.core.localization.Loc
import com.jtool.android.core.models.common.enums.AppScreenEnum
import com.jtool.android.core.models.user.enums.LauncherGestureEnum
import com.jtool.android.ui.navigation.AppProperties

@Composable
fun LauncherGesturesSection(
    gesturesMap: Map<String, String>,
    showGesturesSection: Boolean,
    onToggleShowSection: () -> Unit,
    onAssignGesture: (String, String) -> Unit
) {
    var selectedGestureForPicker by remember { mutableStateOf<LauncherGestureEnum?>(null) }

    Surface(
        onClick = onToggleShowSection,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gesture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = Loc.Launcher.gestures,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = if (showGesturesSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    AnimatedVisibility(
        visible = showGesturesSection,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val gestureList = listOf(
                LauncherGestureEnum.SWIPE_LEFT to (Loc.Launcher.swipeLeft to Icons.AutoMirrored.Filled.ArrowBack),
                LauncherGestureEnum.SWIPE_RIGHT to (Loc.Launcher.swipeRight to Icons.AutoMirrored.Filled.ArrowForward)
            )

            gestureList.forEach { (gesture, info) ->
                val (label, gestureIcon) = info
                val assignedActionId = gesturesMap[gesture.id] ?: ""
                val assignedName = remember(assignedActionId) {
                    getActionName(assignedActionId)
                }
                val assignedIcon = remember(assignedActionId) {
                    getActionIcon(assignedActionId)
                }

                Surface(
                    onClick = { selectedGestureForPicker = gesture },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = gestureIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (assignedIcon != null) {
                                Icon(
                                    imageVector = assignedIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = assignedName.ifEmpty { Loc.Launcher.none },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (assignedActionId.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedGestureForPicker != null) {
        GestureActionPickerSheet(
            gesture = selectedGestureForPicker!!,
            currentActionId = gesturesMap[selectedGestureForPicker!!.id] ?: "",
            onSelectAction = { actionId ->
                onAssignGesture(selectedGestureForPicker!!.id, actionId)
                selectedGestureForPicker = null
            },
            onDismiss = { selectedGestureForPicker = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureActionPickerSheet(
    gesture: LauncherGestureEnum,
    currentActionId: String,
    onSelectAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val gestureLabel = when (gesture) {
        LauncherGestureEnum.SWIPE_LEFT -> Loc.Launcher.swipeLeft
        LauncherGestureEnum.SWIPE_RIGHT -> Loc.Launcher.swipeRight
        LauncherGestureEnum.DOUBLE_TAP -> Loc.Launcher.doubleTap
    }

    val availableTargets = remember {
        listOf(
            "" to Loc.Launcher.none,
            AppScreenEnum.SETTINGS.id to Loc.Launcher.settings,
            AppScreenEnum.SUPPORT_HUB.id to Loc.Launcher.supportHub
        ) + AppProperties.allApps.map { it.id.id to it.getName() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "${Loc.Launcher.selectAction}: $gestureLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            availableTargets.forEach { (actionId, label) ->
                val icon = remember(actionId) { getActionIcon(actionId) }
                Surface(
                    onClick = { onSelectAction(actionId) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (actionId == currentActionId) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (actionId == currentActionId) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (actionId == currentActionId) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun getActionIcon(actionId: String): ImageVector? {
    if (actionId.isEmpty()) return null
    if (actionId == AppScreenEnum.SETTINGS.id) return Icons.Default.Settings
    if (actionId == AppScreenEnum.SUPPORT_HUB.id) return Icons.Default.SupportAgent
    return AppProperties.allApps.find { it.id.id == actionId }?.icon
}

fun getActionName(actionId: String): String {
    if (actionId.isEmpty()) return Loc.Launcher.none
    if (actionId == AppScreenEnum.SETTINGS.id) return Loc.Launcher.settings
    if (actionId == AppScreenEnum.SUPPORT_HUB.id) return Loc.Launcher.supportHub
    val app = AppProperties.allApps.find { it.id.id == actionId }
    return app?.getName() ?: actionId
}
